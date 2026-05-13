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
import android.os.HandlerThread
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * AudioPlaybackCapture로 PCM 16bit mono 44.1kHz 스트림을 받아 [ringSeconds]초 링버퍼 유지.
 *   - 별도 [HandlerThread]에서 [AudioRecord.read] 연속 호출 → 메인/캡처 스레드 영향 없음.
 *   - [snapshot]으로 최근 N초 PCM을 short[]로 복사해서 가져갈 수 있다.
 *   - [SoundDetector]가 1초 간격으로 snapshot 가져가 fingerprint 계산.
 */
class AudioCaptureSession(
    private val context: Context,
    private val projection: MediaProjection,
    private val ringSeconds: Int = 8,
) {
    companion object {
        const val SAMPLE_RATE = 44_100
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val TAG = "AudioCapture"
    }

    private val ringSize: Int = SAMPLE_RATE * ringSeconds
    private val ring: ShortArray = ShortArray(ringSize)
    @Volatile private var writePos: Int = 0
    @Volatile private var totalWritten: Long = 0L

    private var rec: AudioRecord? = null
    private var thread: HandlerThread? = null
    @Volatile private var running = false

    fun isRunning(): Boolean = running

    @SuppressLint("MissingPermission")
    fun start(): Boolean {
        if (running) return true
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w(TAG, "RECORD_AUDIO permission missing — cannot start")
            return false
        }
        val cfg = AudioPlaybackCaptureConfiguration.Builder(projection)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()
        val fmt = AudioFormat.Builder()
            .setEncoding(ENCODING).setSampleRate(SAMPLE_RATE).setChannelMask(CHANNEL).build()
        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        val r = AudioRecord.Builder()
            .setAudioPlaybackCaptureConfig(cfg)
            .setAudioFormat(fmt)
            .setBufferSizeInBytes(maxOf(minBuf * 4, SAMPLE_RATE * 2))
            .build()
        if (r.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed state=${r.state}")
            r.release()
            return false
        }
        rec = r
        r.startRecording()
        running = true
        val t = HandlerThread("AudioCaptureReader").apply { start() }
        thread = t
        android.os.Handler(t.looper).post { pumpLoop() }
        Log.d(TAG, "started — ring=${ringSeconds}s")
        return true
    }

    private fun pumpLoop() {
        val buf = ShortArray(SAMPLE_RATE / 20) // 50ms chunks
        val r = rec ?: return
        while (running) {
            val n = try { r.read(buf, 0, buf.size) } catch (t: Throwable) { -1 }
            if (n <= 0) {
                if (!running) break
                Thread.sleep(10); continue
            }
            writeRing(buf, n)
        }
    }

    @Synchronized
    private fun writeRing(buf: ShortArray, n: Int) {
        var i = 0
        while (i < n) {
            val space = ringSize - writePos
            val chunk = minOf(space, n - i)
            System.arraycopy(buf, i, ring, writePos, chunk)
            writePos = (writePos + chunk) % ringSize
            i += chunk
        }
        totalWritten += n
    }

    /**
     * 최근 [seconds]초 PCM을 새 short[]로 복사. 아직 그만큼 안 쌓였으면 short array shorter.
     */
    @Synchronized
    fun snapshot(seconds: Int): ShortArray {
        val want = SAMPLE_RATE * seconds
        val available = minOf(totalWritten, ringSize.toLong()).toInt()
        val take = minOf(want, available)
        if (take <= 0) return ShortArray(0)
        val out = ShortArray(take)
        val start = ((writePos - take + ringSize) % ringSize)
        if (start + take <= ringSize) {
            System.arraycopy(ring, start, out, 0, take)
        } else {
            val first = ringSize - start
            System.arraycopy(ring, start, out, 0, first)
            System.arraycopy(ring, 0, out, first, take - first)
        }
        return out
    }

    fun stop() {
        if (!running) return
        running = false
        try { rec?.stop() } catch (_: Throwable) {}
        try { rec?.release() } catch (_: Throwable) {}
        rec = null
        try { thread?.quitSafely() } catch (_: Throwable) {}
        thread = null
        Log.d(TAG, "stopped")
    }
}
