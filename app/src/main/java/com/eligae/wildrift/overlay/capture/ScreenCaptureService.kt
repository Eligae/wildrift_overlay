package com.eligae.wildrift.overlay.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.eligae.wildrift.overlay.R

/**
 * 캡처 서비스 — 외부 lifecycle (MediaProjection 권한, foreground 알림, 캡처 주기).
 * 캡처 인프라는 [CaptureSession], OCR/매칭/broadcast는 [OcrProcessor]가 담당.
 */
class ScreenCaptureService : Service() {

    private var session: CaptureSession? = null
    private var ocr: OcrProcessor? = null
    private var projection: MediaProjection? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var stopRequested = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            cleanup()
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_DATA)
        }
        if (resultCode == 0 || data == null) {
            Log.e(TAG, "No projection data, stopping")
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()

        val mpm = getSystemService(MediaProjectionManager::class.java)
        projection = mpm.getMediaProjection(resultCode, data)
        val proj = projection
        if (proj == null) {
            Log.e(TAG, "MediaProjection null")
            stopSelf()
            return START_NOT_STICKY
        }
        session = CaptureSession(this, proj).apply {
            registerCallback(projectionCallback, mainHandler)
            start()
        }
        ocr = OcrProcessor(
            context = this,
            actionLoadingDetected = ACTION_LOADING_DETECTED,
            extraEnemies = EXTRA_ENEMIES,
            onDone = { scheduleNext() },
        )

        // 새 캡처 세션 = 새 게임으로 간주 → 이전 슬롯/anchor 초기화 + 오버레이 view reload.
        resetSessionState()

        isRunning = true
        stopRequested = false
        mainHandler.postDelayed({ captureFrame() }, INITIAL_DELAY_MS)
        return START_NOT_STICKY
    }

    private fun resetSessionState() {
        val prefs = com.eligae.wildrift.overlay.prefs.OverlayPrefs(applicationContext)
        for (i in 1..5) prefs.setSlotChampion(i, null)
        prefs.allyAnchor = emptyList()
        prefs.allyAnchorAtMs = 0L
        val bi = Intent(ACTION_LOADING_DETECTED).apply {
            setPackage(packageName)
            putStringArrayListExtra(EXTRA_ENEMIES, ArrayList())
        }
        sendBroadcast(bi)
        Log.d(TAG, "Session reset (slots + anchor cleared)")
    }

    private fun startInForeground() {
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun captureFrame() {
        if (stopRequested) return
        val bitmap = session?.acquireBitmap()
        if (bitmap == null) {
            mainHandler.postDelayed({ captureFrame() }, 500)
            return
        }
        ocr?.process(bitmap)
    }

    private fun scheduleNext() {
        if (stopRequested) {
            cleanup()
            stopSelf()
            return
        }
        mainHandler.postDelayed({ captureFrame() }, INTERVAL_MS)
    }

    private fun cleanup() {
        session?.release(projectionCallback)
        session = null
        projection = null
        ocr?.close()
        ocr = null
        isRunning = false
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.capture_notification_title),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { setShowBadge(false) }
        nm.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopRequested = true
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "WRCapture"
        private const val CHANNEL_ID = "capture_service"
        private const val NOTIFICATION_ID = 2
        private const val INITIAL_DELAY_MS = 3_000L
        private const val INTERVAL_MS = 3_000L
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val ACTION_LOADING_DETECTED = "com.eligae.wildrift.overlay.LOADING_DETECTED"
        const val EXTRA_ENEMIES = "enemies"

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
