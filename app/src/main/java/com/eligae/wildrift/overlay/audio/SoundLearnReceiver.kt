package com.eligae.wildrift.overlay.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * ADB broadcast 기반 사운드 학습 트리거.
 *   사용법:
 *     adb shell am broadcast -a com.eligae.wildrift.overlay.LEARN_SOUND \
 *       -e label "매치 시작" -e action MATCH_START -e delay 2000 -e duration 2000
 *
 *   동작:
 *     1. broadcast 수신 시 timestamp 기록
 *     2. delay ms 대기 (게임에서 소리 재생 시간 확보, 기본 2000ms)
 *     3. duration ms 분량의 PCM 스냅샷 (기본 2000ms)
 *     4. Mel fingerprint 계산 → SoundTemplate로 저장
 *     5. 등록된 detector reload
 *
 *   학습된 .npfp 파일은:
 *     /sdcard/Android/data/com.eligae.wildrift.overlay/files/sound_triggers/user/
 *   에 저장됨. adb pull로 가져와 assets에 박으면 builtin으로 배포 가능.
 */
class SoundLearnReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION = "com.eligae.wildrift.overlay.LEARN_SOUND"
        private const val TAG = "SoundLearn"
        private val workerThread = HandlerThread("SoundLearn").apply { start() }
        private val handler = Handler(workerThread.looper)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION) return
        val label = intent.getStringExtra("label") ?: "user_${System.currentTimeMillis()}"
        val actionName = intent.getStringExtra("action") ?: "CUSTOM"
        val delayMs = intent.getStringExtra("delay")?.toLongOrNull() ?: 2000L
        val durationMs = intent.getStringExtra("duration")?.toLongOrNull() ?: 2000L
        val threshold = intent.getStringExtra("threshold")?.toFloatOrNull() ?: 0.85f

        val session = AudioSessionHolder.session
        if (session == null || !session.isRunning()) {
            Log.w(TAG, "Audio session not running — start capture first")
            return
        }
        val action = try { TriggerAction.valueOf(actionName) }
        catch (_: Throwable) { TriggerAction.CUSTOM }

        Log.d(TAG, "Learn requested: label='$label' action=$action delay=${delayMs}ms duration=${durationMs}ms")
        handler.postDelayed({
            try {
                val secondsToSnapshot = ((durationMs + 999) / 1000).toInt().coerceAtLeast(1)
                val pcm = session.snapshot(secondsToSnapshot)
                if (pcm.size < MelFingerprint.FRAME_SIZE) {
                    Log.w(TAG, "PCM too short: ${pcm.size} samples")
                    return@postDelayed
                }
                val features = MelFingerprint.compute(pcm)
                if (features.isEmpty()) {
                    Log.w(TAG, "fingerprint compute failed")
                    return@postDelayed
                }
                val id = "user_${System.currentTimeMillis()}"
                val tpl = SoundTemplate(
                    id = id, label = label, action = action,
                    threshold = threshold, features = features, builtin = false,
                )
                val file = SoundTemplateRepo.saveUserTemplate(context.applicationContext, tpl)
                AudioSessionHolder.detector?.reload()
                Log.d(TAG, "Learned: $label → ${file.absolutePath} (${features.size} frames, ${tpl.durationSec()}s)")
            } catch (t: Throwable) {
                Log.e(TAG, "Learn failed", t)
            }
        }, delayMs)
    }
}
