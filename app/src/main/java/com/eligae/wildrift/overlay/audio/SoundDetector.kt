package com.eligae.wildrift.overlay.audio

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.eligae.wildrift.overlay.history.MatchHistoryStore
import com.eligae.wildrift.overlay.history.MatchRecord
import com.eligae.wildrift.overlay.model.MatchResult
import com.eligae.wildrift.overlay.prefs.OverlayPrefs

/**
 * 1초마다 audio ring buffer snapshot → Mel fingerprint → 등록된 템플릿과 매칭 → 트리거 발화.
 *   - 같은 트리거는 cooldown(5s) 내 중복 발화 방지.
 *   - builtin + user 통합. SoundTriggerActivity가 새로 저장하면 [reload] 호출로 갱신.
 */
class SoundDetector(
    private val context: Context,
    private val audio: AudioCaptureSession,
    private val onMatchEnded: (matchId: Long) -> Unit,
) {
    companion object {
        private const val TAG = "SoundDetector"
        private const val POLL_MS = 1_000L
        private const val WINDOW_SECONDS = 8
        private const val COOLDOWN_MS = 5_000L
    }

    @Volatile private var running = false
    private val cooldown = HashMap<String, Long>()
    @Volatile private var templates: List<SoundTemplate> = emptyList()

    private val thread = HandlerThread("SoundDetector").apply { start() }
    private val handler = Handler(thread.looper)

    fun start() {
        if (running) return
        running = true
        reload()
        handler.post(loop)
        Log.d(TAG, "started, templates=${templates.size}")
    }

    fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        thread.quitSafely()
    }

    /** SoundTriggerActivity에서 신규 저장 후 호출해서 템플릿 다시 로드. */
    fun reload() {
        templates = SoundTemplateRepo.loadAll(context)
    }

    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            try { tick() } catch (t: Throwable) { Log.e(TAG, "tick failed", t) }
            if (running) handler.postDelayed(this, POLL_MS)
        }
    }

    private fun tick() {
        if (templates.isEmpty()) return
        val pcm = audio.snapshot(WINDOW_SECONDS)
        if (pcm.size < MelFingerprint.FRAME_SIZE) return
        val features = MelFingerprint.compute(pcm)
        if (features.isEmpty()) return
        val now = System.currentTimeMillis()
        for (tpl in templates) {
            val lastFire = cooldown[tpl.id] ?: 0L
            if (now - lastFire < COOLDOWN_MS) continue
            if (features.size < tpl.frames) continue
            val score = MelFingerprint.bestMatch(features, tpl.features)
            if (score >= tpl.threshold) {
                cooldown[tpl.id] = now
                Log.d(TAG, "TRIGGER fired: ${tpl.label} (action=${tpl.action}) score=$score")
                fire(tpl)
            }
        }
    }

    private fun fire(tpl: SoundTemplate) {
        val prefs = OverlayPrefs(context.applicationContext)
        when (tpl.action) {
            TriggerAction.MATCH_START -> {
                prefs.matchStartedAtMs = System.currentTimeMillis()
                prefs.matchEndDetected = false
                Log.d(TAG, "MATCH_START audio trigger applied")
            }
            TriggerAction.MATCH_WIN, TriggerAction.MATCH_LOSE -> {
                if (prefs.matchStartedAtMs > 0L && !prefs.matchEndDetected) {
                    val id = System.currentTimeMillis()
                    val result = if (tpl.action == TriggerAction.MATCH_WIN) MatchResult.WIN else MatchResult.LOSE
                    val enemies = (1..5).map { prefs.loadSlot(it).championName ?: "" }
                    val allies = (1..5).map { prefs.loadAllySlotChampion(it) ?: "" }
                    val userSlot = prefs.userSlot.takeIf { it >= 0 }
                    val rec = MatchRecord(
                        id = id, startedAtMs = prefs.matchStartedAtMs, endedAtMs = id,
                        result = result, enemies = enemies, allies = allies,
                        userVerified = false, userSlot = userSlot,
                    )
                    MatchHistoryStore(context.applicationContext).add(rec)
                    prefs.matchEndDetected = true
                    prefs.userSlot = -1
                    onMatchEnded(id)
                }
            }
            TriggerAction.ULT_READY -> {
                // 슬롯 매핑은 향후 — 현재는 로그만.
                Log.d(TAG, "ULT_READY trigger (slot mapping not implemented yet)")
            }
            TriggerAction.CHAT_PING -> {
                // 채팅 알림 사운드 검출 = 새 시스템 메시지 등장. 즉시 OCR 1회 → ChatParser가 스펠 사용 잡음.
                com.eligae.wildrift.overlay.capture.ScreenCaptureService.instance
                    ?.triggerImmediateCapture()
                Log.d(TAG, "CHAT_PING — immediate OCR triggered")
            }
            TriggerAction.CUSTOM -> {
                Log.d(TAG, "CUSTOM trigger: ${tpl.label}")
            }
        }
    }
}
