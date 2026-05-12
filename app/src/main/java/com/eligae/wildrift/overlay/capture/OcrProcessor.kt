package com.eligae.wildrift.overlay.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import com.eligae.wildrift.overlay.parse.ChatParser
import com.eligae.wildrift.overlay.parse.LoadingScreenParser
import com.eligae.wildrift.overlay.prefs.OverlayPrefs
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File

/**
 * ML Kit OCR 호출 + ROI crop + ChatParser/LoadingScreenParser 매칭 + anchor 갱신 + broadcast 발신.
 * 캡처 인프라(Session)와 분리되어 bitmap 입력만 받으면 동작.
 */
internal class OcrProcessor(
    private val context: Context,
    /** broadcast action — Service.companion에 박힌 상수를 재사용. */
    private val actionLoadingDetected: String,
    private val extraEnemies: String,
    /** 종료 감지 시 verify-flow 트리거 (MatchRecord id 전달). */
    private val onMatchEnded: (matchId: Long) -> Unit,
    /** OCR 1회가 끝나면 호출 (성공/실패 무관). */
    private val onDone: () -> Unit,
) {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    fun close() {
        try { recognizer.close() } catch (_: Throwable) {}
    }

    fun process(portraitBitmap: Bitmap) {
        val prefs = OverlayPrefs(context.applicationContext)
        val (scaled, rotationDegrees) = prepare(portraitBitmap, prefs)
        val input = InputImage.fromBitmap(scaled, rotationDegrees)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                if (result.textBlocks.isNotEmpty()) {
                    handleResult(result, scaled, prefs)
                    saveBitmap(scaled)
                } else {
                    Log.d(TAG, "OCR empty")
                }
                scaled.recycle()
                onDone()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                scaled.recycle()
                onDone()
            }
    }

    /** ROI가 있으면 회전 + crop, 없으면 portrait 그대로 + rotationDegrees=90. */
    private fun prepare(bitmap: Bitmap, prefs: OverlayPrefs): Pair<Bitmap, Int> {
        return if (prefs.hasCustomRoi) {
            val rotated = BitmapUtils.rotate90(bitmap)
            bitmap.recycle()
            val cropped = BitmapUtils.cropByRatio(
                rotated,
                prefs.roiLeft, prefs.roiTop, prefs.roiRight, prefs.roiBottom,
            )
            rotated.recycle()
            cropped to 0
        } else {
            bitmap to 90
        }
    }

    private fun handleResult(
        result: com.google.mlkit.vision.text.Text,
        scaled: Bitmap,
        prefs: OverlayPrefs,
    ) {
        val n = result.textBlocks.size
        Log.d(TAG, "OCR ok: $n blocks, chars=${result.text.length}, frame=${scaled.width}x${scaled.height}, roi=${prefs.hasCustomRoi}")
        for (block in result.textBlocks) {
            val bb = block.boundingBox
            val bbStr = if (bb != null) "[${bb.left},${bb.top},${bb.right},${bb.bottom}]" else "[null]"
            Log.d(TAG, "BLOCK $bbStr ${block.text.replace("\n", " | ")}")
        }

        val blockTexts = result.textBlocks.map { it.text }
        var chatTouched = false
        for (m in ChatParser.parse(blockTexts)) {
            Log.d(TAG, "CHAT MATCH: ${m.champion} → ${m.spell.name}")
            if (triggerSlotSpell(m, prefs)) chatTouched = true
        }
        if (chatTouched) {
            // 슬롯 prefs 갱신 후 오버레이 view reload broadcast.
            val bi = Intent(actionLoadingDetected).apply {
                setPackage(context.packageName)
                putStringArrayListExtra(extraEnemies, ArrayList())
            }
            context.sendBroadcast(bi)
        }

        // 픽 화면 시그널 — anchor 저장/적팀 broadcast 모두 skip (모든 챔피언 그리드가 매칭돼서 잡음).
        val joined = result.text
        if (PICK_PHASE_SIGNALS.any { joined.contains(it) }) {
            Log.d(TAG, "Skip — pick/lobby phase detected")
            return
        }

        // 종료 감지 — in_match 상태에서 승/패 시그널이 나오면 MatchRecord 저장 후 verify-flow.
        if (prefs.matchStartedAtMs > 0L && !prefs.matchEndDetected) {
            val matchResult = detectEnd(joined)
            if (matchResult != null) {
                val id = System.currentTimeMillis()
                val enemies = (1..5).map { prefs.loadSlot(it).championName ?: "" }
                val allies = (1..5).map { prefs.loadAllySlotChampion(it) ?: "" }
                val record = MatchRecord(
                    id = id,
                    startedAtMs = prefs.matchStartedAtMs,
                    endedAtMs = id,
                    result = matchResult,
                    enemies = enemies,
                    allies = allies,
                    userVerified = false,
                )
                MatchHistoryStore(context.applicationContext).add(record)
                prefs.matchEndDetected = true
                Log.d(TAG, "MATCH END detected: $matchResult, record id=$id")
                onMatchEnded(id)
                return
            }
        }

        val locs = result.textBlocks.mapNotNull { tb ->
            val box = tb.boundingBox ?: return@mapNotNull null
            LoadingScreenParser.TextLoc(
                tb.text,
                (box.left + box.right) / 2f,
                (box.top + box.bottom) / 2f,
            )
        }
        val rotatedFrameHeight = scaled.width
        val anchor = prefs.freshAllyAnchor()
        // ChampionsCache의 모든 한국명을 추가 화이트리스트로 — 정적 KNOWN_NAMES에 없는 챔피언 자동 포함.
        val dynamicNames = ChampionsCache(context.applicationContext).load()
            ?.champions
            ?.map { it.krName }
            ?.toSet()
            ?: emptySet()
        val teams = LoadingScreenParser.parseTeams(locs, rotatedFrameHeight, anchor, dynamicNames)

        maybeSaveAllyAnchor(teams.picks, prefs)
        broadcastEnemiesIfPass(teams, prefs, anchor != null)
    }

    private fun maybeSaveAllyAnchor(
        picks: List<LoadingScreenParser.Pick>,
        prefs: OverlayPrefs,
    ) {
        if (picks.size != 5) return
        val canonical = picks.map { it.canonical }
        if (canonical.toSet() != prefs.allyAnchor.toSet()) {
            prefs.allyAnchor = canonical
            prefs.allyAnchorAtMs = System.currentTimeMillis()
            Log.d(TAG, "ALLY ANCHOR SAVED: $canonical")
        }
    }

    /**
     * 채팅 매칭으로 슬롯의 spell ready를 갱신. 챔피언명 일치 + spell1/2 중 매칭된 쪽이 ready=null이면
     * defaultCooldownSec 만큼 카운트 시작. 이미 ready 박혀있으면 무시 (중복 트리거 방지).
     */
    private fun triggerSlotSpell(m: ChatParser.Match, prefs: OverlayPrefs): Boolean {
        for (i in 1..5) {
            val state = prefs.loadSlot(i)
            if (state.championName != m.champion) continue
            val now = System.currentTimeMillis()
            val cd = m.spell.defaultCooldownSec * 1000L
            val updated = when {
                state.spell1 == m.spell && state.spell1ReadyAtEpochMs == null ->
                    state.copy(spell1ReadyAtEpochMs = now + cd)
                state.spell2 == m.spell && state.spell2ReadyAtEpochMs == null ->
                    state.copy(spell2ReadyAtEpochMs = now + cd)
                else -> null
            }
            if (updated != null) {
                prefs.saveSlot(updated)
                Log.d(TAG, "AUTO SPELL: slot=$i ${m.champion} ${m.spell.name} cd=${cd / 1000}s")
                return true
            }
            return false
        }
        return false
    }

    private fun detectEnd(text: String): MatchResult? {
        return when {
            WIN_SIGNALS.any { text.contains(it) } -> MatchResult.WIN
            LOSE_SIGNALS.any { text.contains(it) } -> MatchResult.LOSE
            else -> null
        }
    }

    private fun broadcastEnemiesIfPass(
        teams: LoadingScreenParser.Teams,
        prefs: OverlayPrefs,
        anchorActive: Boolean,
    ) {
        val enemyCount = teams.enemies.count { it != null }
        val allyCount = teams.allies.count { it != null }
        val pass = if (anchorActive) {
            enemyCount >= 3
        } else {
            enemyCount + allyCount >= 6 && enemyCount >= 3
        }
        if (!pass) return

        Log.d(TAG, "LOADING ENEMIES (TOP→SUP): ${teams.enemies}${if (anchorActive) " [anchor]" else ""}")
        Log.d(TAG, "LOADING ALLIES  (TOP→SUP): ${teams.allies}")
        // 슬롯 5개 라인 순서. incoming이 null이면 기존 슬롯 유지 — 한 번 잡힌 챔피언은 덮어쓰지 않음
        // (이전 캡처에서 5명 잡혔는데 다음 캡처가 4명만 잡으면 슬롯 손실 방지).
        for (i in 0 until 5) {
            teams.enemies.getOrNull(i)?.let { prefs.setSlotChampion(i + 1, it) }
            teams.allies.getOrNull(i)?.let { prefs.setAllySlotChampion(i + 1, it) }
        }
        // 새 게임 시작 마킹 (종료 감지에 쓰임).
        if (prefs.matchStartedAtMs == 0L || prefs.matchEndDetected) {
            prefs.matchStartedAtMs = System.currentTimeMillis()
            prefs.matchEndDetected = false
        }
        val bi = Intent(actionLoadingDetected).apply {
            setPackage(context.packageName)
            putStringArrayListExtra(
                extraEnemies,
                ArrayList(teams.enemies.map { it ?: "" }),
            )
        }
        context.sendBroadcast(bi)
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            val dir = context.getExternalFilesDir(null)
            val file = File(dir, "capture_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
            Log.d(TAG, "Saved: ${file.name} (${bitmap.width}x${bitmap.height})")
        } catch (t: Throwable) {
            Log.e(TAG, "Save failed", t)
        }
    }

    companion object {
        private const val TAG = "WRCapture"
        private val PICK_PHASE_SIGNALS = listOf(
            "챔피언을 선택하세요",
            "다른 플레이어를 기다리는 중",
            "챔피언 변경",
            "재시작",
        )
        private val WIN_SIGNALS = listOf("승리", "VICTORY", "Victory")
        private val LOSE_SIGNALS = listOf("패배", "DEFEAT", "Defeat")
    }
}
