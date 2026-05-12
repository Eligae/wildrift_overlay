package com.eligae.wildrift.overlay

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var stopRequested = false

    private val recognizer by lazy {
        TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
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
        mediaProjection = mpm.getMediaProjection(resultCode, data)
        if (mediaProjection == null) {
            Log.e(TAG, "MediaProjection null")
            stopSelf()
            return START_NOT_STICKY
        }
        mediaProjection?.registerCallback(projectionCallback, mainHandler)
        setupCapture()

        isRunning = true
        stopRequested = false
        mainHandler.postDelayed({ captureFrame() }, INITIAL_DELAY_MS)
        return START_NOT_STICKY
    }

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            super.onStop()
            cleanup()
        }
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
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notif)
        }
    }

    private fun setupCapture() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi
        Log.d(TAG, "Display ${width}x${height} dpi=$density")

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "WRCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null,
        )
    }

    private fun captureFrame() {
        if (stopRequested) return
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage()
        if (image == null) {
            mainHandler.postDelayed({ captureFrame() }, 500)
            return
        }
        val bitmap = try {
            imageToBitmap(image)
        } catch (t: Throwable) {
            Log.e(TAG, "Bitmap convert failed", t)
            image.close()
            scheduleNext()
            return
        }
        image.close()
        runOcr(bitmap)
    }

    private fun rotate90(src: Bitmap): Bitmap {
        val m = android.graphics.Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888,
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }

    private fun saveBitmap(bitmap: Bitmap) {
        try {
            val dir = getExternalFilesDir(null)
            val file = File(dir, "capture_${System.currentTimeMillis()}.png")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
            Log.d(TAG, "Saved: ${file.name} (${bitmap.width}x${bitmap.height})")
        } catch (t: Throwable) {
            Log.e(TAG, "Save failed", t)
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        val prefs = OverlayPrefs(applicationContext)
        // 사용자 ROI가 있으면 회전 frame에서 잘라 OCR. 아니면 portrait 원본 + rotationDegrees=90.
        val rotated = if (prefs.hasCustomRoi) rotate90(bitmap).also { bitmap.recycle() } else bitmap
        val scaled = if (prefs.hasCustomRoi) {
            val w = rotated.width
            val h = rotated.height
            val l = (w * prefs.roiLeft).toInt().coerceIn(0, w - 1)
            val t = (h * prefs.roiTop).toInt().coerceIn(0, h - 1)
            val r = (w * prefs.roiRight).toInt().coerceIn(l + 1, w)
            val b = (h * prefs.roiBottom).toInt().coerceIn(t + 1, h)
            val cropped = Bitmap.createBitmap(rotated, l, t, r - l, b - t)
            rotated.recycle()
            cropped
        } else rotated
        val rotationDegrees = if (prefs.hasCustomRoi) 0 else 90
        val input = InputImage.fromBitmap(scaled, rotationDegrees)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                val n = result.textBlocks.size
                if (n > 0) {
                    Log.d(TAG, "OCR ok: $n blocks, chars=${result.text.length}, frame=${scaled.width}x${scaled.height}, roi=${prefs.hasCustomRoi}")
                    for (block in result.textBlocks) {
                        val bb = block.boundingBox
                        val bbStr = if (bb != null) "[${bb.left},${bb.top},${bb.right},${bb.bottom}]" else "[null]"
                        Log.d(TAG, "BLOCK $bbStr ${block.text.replace("\n", " | ")}")
                    }

                    val blockTexts = result.textBlocks.map { it.text }
                    val chatMatches = ChatParser.parse(blockTexts)
                    for (m in chatMatches) {
                        Log.d(TAG, "CHAT MATCH: ${m.champion} → ${m.spell.name}")
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
                    val overlayPrefs = OverlayPrefs(applicationContext)
                    val anchor = overlayPrefs.freshAllyAnchor()
                    val teams = LoadingScreenParser.parseTeams(locs, rotatedFrameHeight, anchor)

                    // 5명 화면 anchor 저장 — picks가 정확히 5명이고 anchor 미보유 또는 다른 5명일 때.
                    // (인게임에서 5명 동시 매칭은 거의 안 일어남)
                    if (teams.picks.size == 5) {
                        val canonical = teams.picks.map { it.canonical }
                        if (canonical.toSet() != overlayPrefs.allyAnchor.toSet()) {
                            overlayPrefs.allyAnchor = canonical
                            overlayPrefs.allyAnchorAtMs = System.currentTimeMillis()
                            Log.d(TAG, "ALLY ANCHOR SAVED: $canonical")
                        }
                    }

                    // anchor 있으면 enemies 3명 이상이면 broadcast (10명 중 일부 OCR 누락 허용).
                    // anchor 없으면 fallback — 적+동 합 6명 이상 + 적 3명 이상.
                    val passBroadcast = if (anchor != null) {
                        teams.enemies.size >= 3
                    } else {
                        teams.enemies.size + teams.allies.size >= 6 && teams.enemies.size >= 3
                    }
                    if (passBroadcast) {
                        Log.d(TAG, "LOADING ENEMIES (TOP→SUP): ${teams.enemies}${if (anchor != null) " [anchor]" else ""}")
                        Log.d(TAG, "LOADING ALLIES  (TOP→SUP): ${teams.allies}")
                        teams.enemies.forEachIndexed { i, name ->
                            if (i + 1 <= 5) overlayPrefs.setSlotChampion(i + 1, name)
                        }
                        for (i in (teams.enemies.size + 1)..5) {
                            overlayPrefs.setSlotChampion(i, null)
                        }
                        val bi = Intent(ACTION_LOADING_DETECTED).apply {
                            setPackage(packageName)
                            putStringArrayListExtra(EXTRA_ENEMIES, ArrayList(teams.enemies))
                        }
                        sendBroadcast(bi)
                    }
                    // 학습용 — 텍스트 있는 모든 캡처 저장 (scaled 0.5x, ~150KB)
                    saveBitmap(scaled)
                } else {
                    Log.d(TAG, "OCR empty")
                }
                scaled.recycle()
                scheduleNext()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                scaled.recycle()
                scheduleNext()
            }
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
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Throwable) {}
        imageReader = null
        try { mediaProjection?.unregisterCallback(projectionCallback) } catch (_: Throwable) {}
        try { mediaProjection?.stop() } catch (_: Throwable) {}
        mediaProjection = null
        isRunning = false
    }

    private fun createChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.capture_notification_title),
            NotificationManager.IMPORTANCE_LOW
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
