package com.eligae.wildrift.overlay.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.util.Log

/**
 * MediaProjection · VirtualDisplay · ImageReader 셋업·해제 + 1 프레임 acquire 책임.
 * Service lifecycle와 분리되어 가능한 최소의 외부 의존만 가진다.
 */
internal class CaptureSession(
    private val context: Context,
    private val mediaProjection: MediaProjection,
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    fun start() {
        val metrics = context.resources.displayMetrics
        // WR은 landscape 고정 → surface도 landscape (큰 쪽 W, 작은 쪽 H)로 강제.
        // 그래야 게임 픽셀이 letterbox 없이 surface 전체에 fit, rotate90 후 정상 비율.
        val width = maxOf(metrics.widthPixels, metrics.heightPixels)
        val height = minOf(metrics.widthPixels, metrics.heightPixels)
        val density = metrics.densityDpi
        Log.d(TAG, "Display ${width}x${height} dpi=$density (forced landscape)")

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "WRCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null, null,
        )
    }

    fun registerCallback(callback: MediaProjection.Callback, handler: Handler) {
        mediaProjection.registerCallback(callback, handler)
    }

    /** 최신 프레임 1장을 portrait bitmap으로 반환. 없으면 null. */
    fun acquireBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        val image = reader.acquireLatestImage() ?: return null
        return try {
            BitmapUtils.imageToBitmap(image).also { image.close() }
        } catch (t: Throwable) {
            Log.e(TAG, "Bitmap convert failed", t)
            image.close()
            null
        }
    }

    fun release(projectionCallback: MediaProjection.Callback) {
        try { virtualDisplay?.release() } catch (_: Throwable) {}
        virtualDisplay = null
        try { imageReader?.close() } catch (_: Throwable) {}
        imageReader = null
        try { mediaProjection.unregisterCallback(projectionCallback) } catch (_: Throwable) {}
        try { mediaProjection.stop() } catch (_: Throwable) {}
    }

    companion object {
        private const val TAG = "WRCapture"
    }
}
