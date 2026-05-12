package com.eligae.wildrift.overlay.capture

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.Image

/**
 * MediaProjection 캡처/회전/크롭에 쓰는 bitmap 변환 헬퍼.
 * Service 내부 작업이 아닌 순수 변환이라 외부 의존이 없다.
 */
internal object BitmapUtils {

    fun imageToBitmap(image: Image): Bitmap {
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

    fun rotate90(src: Bitmap): Bitmap {
        val m = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
    }

    /**
     * 회전된 frame에서 비율 ROI(0~1)로 잘라낸 새 bitmap. 원본은 recycle 하지 않는다.
     */
    fun cropByRatio(
        src: Bitmap,
        leftR: Float,
        topR: Float,
        rightR: Float,
        bottomR: Float,
    ): Bitmap {
        val w = src.width
        val h = src.height
        val l = (w * leftR).toInt().coerceIn(0, w - 1)
        val t = (h * topR).toInt().coerceIn(0, h - 1)
        val r = (w * rightR).toInt().coerceIn(l + 1, w)
        val b = (h * bottomR).toInt().coerceIn(t + 1, h)
        return Bitmap.createBitmap(src, l, t, r - l, b - t)
    }
}
