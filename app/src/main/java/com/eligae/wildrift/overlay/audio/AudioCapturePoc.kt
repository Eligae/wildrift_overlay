package com.eligae.wildrift.overlay.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.util.Log
import androidx.core.content.ContextCompat
import kotlin.math.abs

/**
 * Phase 0 PoC — WR 재생 오디오가 캡처되는지 검증.
 *   - MediaProjection 토큰을 [start]에 받아 AudioPlaybackCapture 셋업
 *   - durationMs 동안 PCM 16bit mono 44.1kHz 받아서 평균 |amplitude| 계산
 *   - 결과를 logcat (`AudioCapturePoc`) 에 출력. 평균 진폭 100 이상이면 정상 캡처.
 *   - 0~수십 이면 WR이 ALLOW_CAPTURE_BY_NONE으로 막은 것 → 무음.
 */
object AudioCapturePoc {

    private const val TAG = "AudioCapturePoc"
    private const val SAMPLE_RATE = 44_100
    private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
    private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

    @SuppressLint("MissingPermission") // 권한 체크는 [hasPermission]에서.
    fun start(context: Context, projection: MediaProjection, durationMs: Long = 5_000L) {
        if (!hasPermission(context)) {
            Log.w(TAG, "RECORD_AUDIO permission missing — skip PoC")
            return
        }
        Thread({
            try {
                runCapture(projection, durationMs)
            } catch (t: Throwable) {
                Log.e(TAG, "PoC failed", t)
            }
        }, "AudioCapturePoc").start()
    }

    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    private fun runCapture(projection: MediaProjection, durationMs: Long) {
        val cfg = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val fmt = AudioFormat.Builder()
            .setEncoding(ENCODING)
            .setSampleRate(SAMPLE_RATE)
            .setChannelMask(CHANNEL)
            .build()
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        val bufSize = maxOf(minBuf * 4, SAMPLE_RATE * 2) // ≥ 1s
        val rec = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(cfg)
            .setAudioFormat(fmt)
            .setBufferSizeInBytes(bufSize)
            .build()
        rec.startRecording()
        Log.d(TAG, "AudioRecord state=${rec.state}, recordingState=${rec.recordingState}, bufSize=$bufSize")

        val buf = ShortArray(SAMPLE_RATE / 10) // 100ms 단위
        var samplesTotal = 0L
        var sumAbs = 0L
        var peakAbs = 0
        val deadline = System.currentTimeMillis() + durationMs
        while (System.currentTimeMillis() < deadline) {
            val n = rec.read(buf, 0, buf.size)
            if (n <= 0) {
                Log.w(TAG, "read returned $n")
                Thread.sleep(50)
                continue
            }
            for (i in 0 until n) {
                val v = abs(buf[i].toInt())
                sumAbs += v
                if (v > peakAbs) peakAbs = v
            }
            samplesTotal += n
        }
        rec.stop(); rec.release()

        val avgAbs = if (samplesTotal > 0) sumAbs.toDouble() / samplesTotal else 0.0
        Log.d(TAG, "PoC done. samples=$samplesTotal, avg|x|=${"%.1f".format(avgAbs)}, peak=$peakAbs")
        when {
            samplesTotal == 0L -> Log.w(TAG, "RESULT: NO_DATA — read 실패. AudioRecord 초기화 문제 가능.")
            avgAbs < 5.0 -> Log.w(TAG, "RESULT: SILENT — WR이 ALLOW_CAPTURE_BY_NONE으로 막거나 게임 음소거.")
            avgAbs < 100.0 -> Log.w(TAG, "RESULT: WEAK — 신호 약함. 게임 볼륨 확인 필요.")
            else -> Log.d(TAG, "RESULT: OK — 캡처 정상 ✓")
        }
    }
}
