package com.eligae.wildrift.overlay.floating

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 캘리브레이션 ROI — 비율 좌표(0~1)로 보유, 화면 픽셀에 mapping해서 그림.
 * 터치:
 *   - 모서리 4개 핸들 — 해당 모서리 드래그(리사이즈).
 *   - 변(에지) 근처 — 그 변 드래그(단일 축 리사이즈).
 *   - 내부 — 전체 이동.
 *   - 외부 — 그 지점에서 새 사각형 시작(drag-out).
 */
class RoiOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    private val stroke = Paint().apply {
        color = Color.parseColor("#FF6633")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val fill = Paint().apply {
        color = Color.parseColor("#330AC8B9")
        style = Paint.Style.FILL
    }
    private val handle = Paint().apply {
        color = Color.parseColor("#FFC89B3C")
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val handleStroke = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    /** 비율 좌표 (0~1). */
    var roi: RectF = RectF(0.1f, 0.1f, 0.9f, 0.9f)
        set(value) {
            field = clamp(value)
            invalidate()
            listener?.invoke(field)
        }

    /** roi 변경 콜백. */
    var listener: ((RectF) -> Unit)? = null

    private val handleRadiusPx: Float
    private val edgeTolerancePx: Float

    init {
        val d = resources.displayMetrics.density
        handleRadiusPx = 18f * d
        edgeTolerancePx = 30f * d
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat(); val h = height.toFloat()
        val l = roi.left * w; val t = roi.top * h
        val r = roi.right * w; val b = roi.bottom * h
        canvas.drawRect(l, t, r, b, fill)
        canvas.drawRect(l, t, r, b, stroke)
        for ((cx, cy) in listOf(l to t, r to t, l to b, r to b)) {
            canvas.drawCircle(cx, cy, handleRadiusPx, handle)
            canvas.drawCircle(cx, cy, handleRadiusPx, handleStroke)
        }
    }

    private enum class Grab { TL, TR, BL, BR, T, B, L, R, INSIDE, DRAW_NEW, NONE }

    private var grab: Grab = Grab.NONE
    private var grabStartX = 0f
    private var grabStartY = 0f
    private val grabStartRoi = RectF()

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return false
        val px = event.x; val py = event.y
        val l = roi.left * w; val t = roi.top * h
        val r = roi.right * w; val b = roi.bottom * h
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                grab = classify(px, py, l, t, r, b)
                grabStartX = px; grabStartY = py
                grabStartRoi.set(roi)
                if (grab == Grab.DRAW_NEW) {
                    roi = RectF(px / w, py / h, px / w, py / h)
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (grab == Grab.NONE) return false
                applyDrag(px, py, w, h)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                grab = Grab.NONE
                if (roi.width() < 0.02f || roi.height() < 0.02f) {
                    roi = RectF(0.1f, 0.1f, 0.9f, 0.9f)
                }
                return true
            }
        }
        return false
    }

    private fun classify(x: Float, y: Float, l: Float, t: Float, r: Float, b: Float): Grab {
        val rH = handleRadiusPx + 4f
        if (dist(x, y, l, t) <= rH) return Grab.TL
        if (dist(x, y, r, t) <= rH) return Grab.TR
        if (dist(x, y, l, b) <= rH) return Grab.BL
        if (dist(x, y, r, b) <= rH) return Grab.BR
        val inX = x in l..r
        val inY = y in t..b
        if (inX && kotlin.math.abs(y - t) <= edgeTolerancePx) return Grab.T
        if (inX && kotlin.math.abs(y - b) <= edgeTolerancePx) return Grab.B
        if (inY && kotlin.math.abs(x - l) <= edgeTolerancePx) return Grab.L
        if (inY && kotlin.math.abs(x - r) <= edgeTolerancePx) return Grab.R
        if (inX && inY) return Grab.INSIDE
        return Grab.DRAW_NEW
    }

    private fun applyDrag(px: Float, py: Float, w: Float, h: Float) {
        val rx = (px / w).coerceIn(0f, 1f)
        val ry = (py / h).coerceIn(0f, 1f)
        val dx = (px - grabStartX) / w
        val dy = (py - grabStartY) / h
        val s = grabStartRoi
        roi = when (grab) {
            Grab.TL -> RectF(rx, ry, s.right, s.bottom)
            Grab.TR -> RectF(s.left, ry, rx, s.bottom)
            Grab.BL -> RectF(rx, s.top, s.right, ry)
            Grab.BR -> RectF(s.left, s.top, rx, ry)
            Grab.T -> RectF(s.left, ry, s.right, s.bottom)
            Grab.B -> RectF(s.left, s.top, s.right, ry)
            Grab.L -> RectF(rx, s.top, s.right, s.bottom)
            Grab.R -> RectF(s.left, s.top, rx, s.bottom)
            Grab.INSIDE -> {
                val rw = s.width(); val rh = s.height()
                val nl = (s.left + dx).coerceIn(0f, 1f - rw)
                val nt = (s.top + dy).coerceIn(0f, 1f - rh)
                RectF(nl, nt, nl + rw, nt + rh)
            }
            Grab.DRAW_NEW -> {
                val x0 = (grabStartX / w).coerceIn(0f, 1f)
                val y0 = (grabStartY / h).coerceIn(0f, 1f)
                RectF(minOf(x0, rx), minOf(y0, ry), maxOf(x0, rx), maxOf(y0, ry))
            }
            Grab.NONE -> s
        }
    }

    private fun clamp(r: RectF): RectF {
        val l = r.left.coerceIn(0f, 1f)
        val t = r.top.coerceIn(0f, 1f)
        val ri = r.right.coerceIn(0f, 1f)
        val bo = r.bottom.coerceIn(0f, 1f)
        return RectF(minOf(l, ri), minOf(t, bo), maxOf(l, ri), maxOf(t, bo))
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float =
        kotlin.math.hypot(x1 - x2, y1 - y2)
}
