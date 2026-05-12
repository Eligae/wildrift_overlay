package com.eligae.wildrift.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class OverlayView(
    context: Context,
    private val prefs: OverlayPrefs,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit,
) : LinearLayout(context) {

    private val handleView: TextView
    private val slotsContainer: LinearLayout
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val scale = prefs.scale

    init {
        orientation = HORIZONTAL
        applyBg(prefs.bgAlpha, accent = false)
        setPadding(dp(3), dp(3), dp(3), dp(3))

        handleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 14f * scale
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(22), LayoutParams.MATCH_PARENT)
        }
        addView(handleView)

        slotsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        for (i in 1..5) {
            slotsContainer.addView(SlotView(context, i, prefs))
        }
        addView(slotsContainer)

        applyLevel()
        attachHandleGestures(handleView)
    }

    private fun dp(v: Int): Int = (v * scale * resources.displayMetrics.density).toInt()

    @SuppressLint("ClickableViewAccessibility")
    private fun attachHandleGestures(handle: TextView) {
        var downX = 0f
        var downY = 0f
        var moved = false

        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    moved = false
                    onDragStart()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        moved = true
                    }
                    if (moved) onDrag(dx, dy)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) onDragEnd() else cycleLevel()
                    true
                }
                MotionEvent.ACTION_CANCEL -> {
                    if (moved) onDragEnd()
                    true
                }
                else -> false
            }
        }
    }

    private fun cycleLevel() {
        prefs.collapseLevel = (prefs.collapseLevel + 1) % 3
        applyLevel()
    }

    private fun applyLevel() {
        val level = prefs.collapseLevel
        handleView.text = HANDLE_LABELS[level]
        slotsContainer.visibility = if (level == 2) View.GONE else View.VISIBLE
        for (i in 0 until slotsContainer.childCount) {
            (slotsContainer.getChildAt(i) as? SlotView)?.setLaneLabelVisible(level == 0)
        }
    }

    /** broadcast 받았을 때 슬롯 전체 다시 load. */
    fun reloadAll() {
        for (i in 0 until slotsContainer.childCount) {
            (slotsContainer.getChildAt(i) as? SlotView)?.reload()
        }
    }

    /**
     * 배경 색·알파 즉시 갱신. accent=true면 hex teal (슬라이더 조작 중 미리보기).
     */
    fun applyBg(alpha: Float, accent: Boolean) {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val color = if (accent) Color.argb(a, 0x0A, 0xC8, 0xB9) else Color.argb(a, 0, 0, 0)
        setBackgroundColor(color)
    }

    companion object {
        private val HANDLE_LABELS = arrayOf("−", "≡", "+")
    }
}
