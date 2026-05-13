package com.eligae.wildrift.overlay.capture

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import com.eligae.wildrift.overlay.api.ChampionSkinsCache
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import com.eligae.wildrift.overlay.model.Spell
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
    private val spellMatcher by lazy { SpellMatcher(context.applicationContext) }

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

    /**
     * 항상 rotate90 적용 후 처리. ML Kit과 후속 crop(SpellMatcher 등)이 같은 좌표계를 공유.
     * rotationDegrees는 항상 0 — 결과 boundingBox는 rotated frame 기준.
     */
    private fun prepare(bitmap: Bitmap, prefs: OverlayPrefs): Pair<Bitmap, Int> {
        val rotated = BitmapUtils.rotate90(bitmap)
        bitmap.recycle()
        return if (prefs.hasCustomRoi) {
            val cropped = BitmapUtils.cropByRatio(
                rotated,
                prefs.roiLeft, prefs.roiTop, prefs.roiRight, prefs.roiBottom,
            )
            rotated.recycle()
            cropped to 0
        } else {
            rotated to 0
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
        val dynamicForChat = ChampionsCache(context.applicationContext).load()
            ?.champions?.map { it.krName }?.toSet() ?: emptySet()
        val skinAliasMap = ChampionSkinsCache(context.applicationContext).flatten()
        var chatTouched = false
        for (m in ChatParser.parse(blockTexts, dynamicForChat, skinAliasMap)) {
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
        val rotatedFrameHeight = scaled.height
        val anchor = prefs.freshAllyAnchor()
        // ChampionsCache의 모든 한국명을 추가 화이트리스트로 — 정적 KNOWN_NAMES에 없는 챔피언 자동 포함.
        val dynamicNames = ChampionsCache(context.applicationContext).load()
            ?.champions
            ?.map { it.krName }
            ?.toSet()
            ?: emptySet()
        val teams = LoadingScreenParser.parseTeams(locs, rotatedFrameHeight, anchor, dynamicNames, skinAliasMap)

        maybeSaveAllyAnchor(teams.picks, prefs)
        broadcastEnemiesIfPass(scaled, teams, prefs, anchor != null)
    }

    /**
     * 픽 직후 우리 팀 5명만 보여주는 화면 → anchor. 풀로딩(10명)에서 OCR 누락된 5명을 잘못
     * anchor로 박는 사고를 막기 위해 picks가 한 column에 모여있을 때만 저장. 풀로딩 화면에서는
     * 적팀·아군 column이 양쪽으로 흩어져 x 분산이 크다 — 그땐 anchor 갱신 skip.
     * 추가 안전판: 진행 중 게임(matchStartedAtMs > 0)일 땐 새 anchor 박지 않음 — 한 게임 한 anchor.
     */
    private fun maybeSaveAllyAnchor(
        picks: List<LoadingScreenParser.Pick>,
        prefs: OverlayPrefs,
    ) {
        if (picks.size != 5) return
        if (prefs.matchStartedAtMs > 0L && !prefs.matchEndDetected) return
        val xs = picks.map { it.centerX }
        val xSpread = xs.max() - xs.min()
        if (xSpread > ANCHOR_X_SPREAD_MAX) {
            Log.d(TAG, "ANCHOR SKIP: x spread=$xSpread > $ANCHOR_X_SPREAD_MAX (likely loading 10-name screen)")
            return
        }
        val canonical = picks.map { it.canonical }
        if (canonical.toSet() != prefs.allyAnchor.toSet()) {
            prefs.allyAnchor = canonical
            prefs.allyAnchorAtMs = System.currentTimeMillis()
            Log.d(TAG, "ALLY ANCHOR SAVED: $canonical (xSpread=$xSpread)")
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
        scaled: Bitmap,
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
        detectEnemySpells(scaled, teams, prefs)
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

    /**
     * 풀로딩 카드의 적팀 스펠 두 개를 이미지 매칭으로 판별 → SlotState.spell1/2 갱신.
     * 카드 layout (실측): 카드 안 좌측 1/4 같은 세로 column에 위→아래로 IGNITE(s1)·FLASH(s2)·
     * 챔피언 초상화·이름 라벨·라인 아이콘 순. 라벨 cx에서 거의 동일한 x로 두 스펠 박스 crop.
     */
    private fun detectEnemySpells(
        scaled: Bitmap,
        teams: LoadingScreenParser.Teams,
        prefs: OverlayPrefs,
    ) {
        val H = scaled.height
        if (H <= 0) return
        val nameToPick = teams.picks.associateBy { it.canonical }
        val size = (H * SPELL_BOX_H_RATIO).toInt().coerceAtLeast(8)
        val dx = (H * SPELL_DX_RATIO).toInt()
        val dy1 = (H * SPELL_DY1_RATIO).toInt()
        val dy2 = (H * SPELL_DY2_RATIO).toInt()
        for (slotIdx in 0 until 5) {
            val champ = teams.enemies.getOrNull(slotIdx) ?: continue
            val pick = nameToPick[champ] ?: continue
            val cx = pick.centerX.toInt()
            val cy = pick.centerY.toInt()
            val sx = cx + dx
            val s1 = matchSpellAt(scaled, sx, cy - dy1, size)
            val s2 = matchSpellAt(scaled, sx, cy - dy2, size)
            if (s1 == null && s2 == null) continue
            val current = prefs.loadSlot(slotIdx + 1)
            val updated = current.copy(
                spell1 = s1 ?: current.spell1,
                spell2 = s2 ?: current.spell2,
            )
            prefs.saveSlot(updated)
            Log.d(TAG, "ENEMY SPELL: slot=${slotIdx + 1} $champ s1=${s1?.name ?: "-"} s2=${s2?.name ?: "-"}")
        }
    }

    private fun matchSpellAt(scaled: Bitmap, cx: Int, cy: Int, size: Int): Spell? {
        val half = size / 2
        val x = (cx - half).coerceIn(0, scaled.width - size)
        val y = (cy - half).coerceIn(0, scaled.height - size)
        if (x < 0 || y < 0) return null
        val crop = Bitmap.createBitmap(scaled, x, y, size, size)
        return try { spellMatcher.match(crop) } finally { crop.recycle() }
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

        // 픽 화면(한 column, x 분산 작음) vs 풀로딩(양쪽 column, x 분산 큼) 구분 임계값.
        // 실측: 픽 화면은 한 column 평균 x ≈ 1006 (분산 < 10px), 풀로딩 enemy/ally column 차 ≈ 240px.
        private const val ANCHOR_X_SPREAD_MAX = 150f

        // 카드 안 스펠 박스 비율 (rotated frame H=1088 실측, 2026-05-13 캡처).
        // 카드 안 layout: 좌측에 스펠 sub-column(IGNITE 위, FLASH 아래), 그 우측에 챔피언 일러스트
        // sub-column(이름 라벨이 그 안쪽). 라벨 cx 기준 스펠은 좌측 54px·위쪽 48px(s1)/33px(s2).
        private const val SPELL_BOX_H_RATIO = 0.020f   // ~22px
        private const val SPELL_DX_RATIO = -0.050f     // ~54px 좌측 (라벨 → 스펠 sub-column)
        private const val SPELL_DY1_RATIO = 0.044f     // ~48px 위 (IGNITE 위치)
        private const val SPELL_DY2_RATIO = 0.030f     // ~33px 위 (FLASH 위치)
    }
}
