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
        mainHandler.postDelayed({ captureFrame() }, CAPTURE_DELAY_MS)
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
        val reader = imageReader ?: return
        val image = reader.acquireLatestImage()
        if (image == null) {
            Log.w(TAG, "No image available, retry in 500ms")
            mainHandler.postDelayed({ captureFrame() }, 500)
            return
        }
        val bitmap = try {
            imageToBitmap(image)
        } catch (t: Throwable) {
            Log.e(TAG, "Bitmap convert failed", t)
            image.close()
            finishWith()
            return
        }
        image.close()
        saveBitmap(bitmap)
        runOcr(bitmap)
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
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 90, it) }
            Log.d(TAG, "Saved: ${file.absolutePath} (${bitmap.width}x${bitmap.height})")
        } catch (t: Throwable) {
            Log.e(TAG, "Save failed", t)
        }
    }

    private fun runOcr(bitmap: Bitmap) {
        // 게임 콘텐츠가 가로 → portrait 캡처라 90도 회전 메타.
        val input = InputImage.fromBitmap(bitmap, 90)
        recognizer.process(input)
            .addOnSuccessListener { result ->
                Log.d(TAG, "OCR ok: ${result.textBlocks.size} blocks, chars=${result.text.length}")
                val blockTexts = result.textBlocks.map { it.text }
                for (block in blockTexts) {
                    Log.d(TAG, "BLOCK: ${block.replace("\n", " | ")}")
                }
                val chatMatches = ChatParser.parse(blockTexts)
                if (chatMatches.isEmpty()) {
                    Log.d(TAG, "CHAT MATCH: none")
                } else {
                    for (m in chatMatches) {
                        Log.d(TAG, "CHAT MATCH: ${m.champion} → ${m.spell.name}")
                    }
                }

                // 로딩 화면 분석 — 좌표 기반 적팀/아군 분리.
                val locs = result.textBlocks.mapNotNull { tb ->
                    val box = tb.boundingBox ?: return@mapNotNull null
                    LoadingScreenParser.TextLoc(
                        tb.text,
                        (box.left + box.right) / 2f,
                        (box.top + box.bottom) / 2f,
                    )
                }
                // rotationDegrees=90 → ML Kit 좌표는 회전 frame 기준.
                // 원본 bitmap.width = 회전된 frame height.
                val rotatedFrameHeight = bitmap.width
                val teams = LoadingScreenParser.parseTeams(locs, rotatedFrameHeight)
                if (teams.enemies.isNotEmpty()) {
                    Log.d(TAG, "LOADING ENEMIES (TOP→SUP): ${teams.enemies}")
                    Log.d(TAG, "LOADING ALLIES  (TOP→SUP): ${teams.allies}")

                    val overlayPrefs = OverlayPrefs(applicationContext)
                    teams.enemies.forEachIndexed { i, name ->
                        if (i + 1 <= 5) overlayPrefs.setSlotChampion(i + 1, name)
                    }
                    // 슬롯 비어 있으면 명시적으로 비움
                    for (i in (teams.enemies.size + 1)..5) {
                        overlayPrefs.setSlotChampion(i, null)
                    }

                    val intent = Intent(ACTION_LOADING_DETECTED).apply {
                        setPackage(packageName)
                        putStringArrayListExtra(EXTRA_ENEMIES, ArrayList(teams.enemies))
                    }
                    sendBroadcast(intent)
                }
                bitmap.recycle()
                finishWith()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed", e)
                bitmap.recycle()
                finishWith()
            }
    }

    private fun finishWith() {
        cleanup()
        stopSelf()
    }

    private fun cleanup() {
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Throwable) {}
        imageReader = null
        try { mediaProjection?.unregisterCallback(projectionCallback) } catch (_: Throwable) {}
        try { mediaProjection?.stop() } catch (_: Throwable) {}
        mediaProjection = null
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
        cleanup()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "WRCapture"
        private const val CHANNEL_ID = "capture_service"
        private const val NOTIFICATION_ID = 2
        private const val CAPTURE_DELAY_MS = 5000L
        const val EXTRA_RESULT_CODE = "resultCode"
        const val EXTRA_DATA = "data"
        const val ACTION_LOADING_DETECTED = "com.eligae.wildrift.overlay.LOADING_DETECTED"
        const val EXTRA_ENEMIES = "enemies"

        fun start(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_DATA, data)
            }
            context.startForegroundService(intent)
        }
    }
}
