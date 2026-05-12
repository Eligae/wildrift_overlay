package com.eligae.wildrift.overlay.floating

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * 캘리브레이션 화면에서 ImageView 위에 ROI 사각형(비율 좌표)을 빨간 stroke로 그려준다.
 */
class RoiOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val stroke = Paint().apply {
        color = Color.parseColor("#FF6633")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }
    private val fill = Paint().apply {
        color = Color.parseColor("#330AC8B9")
        style = Paint.Style.FILL
    }

    /** 비율 좌표 (0~1). */
    var roi: RectF = RectF(0f, 0f, 1f, 1f)
        set(value) {
            field = value
            invalidate()
        }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val left = roi.left * w
        val top = roi.top * h
        val right = roi.right * w
        val bottom = roi.bottom * h
        canvas.drawRect(left, top, right, bottom, fill)
        canvas.drawRect(left, top, right, bottom, stroke)
    }
}
