package com.eligae.wrspellcheck

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class OverlayView(
    context: Context,
    private val prefs: OverlayPrefs,
    private val onDragStart: () -> Unit,
    private val onDrag: (dx: Int, dy: Int) -> Unit,
    private val onDragEnd: () -> Unit,
) : LinearLayout(context) {

    private val slotsContainer: LinearLayout

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.parseColor("#CC000000"))
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        setPadding(dp(3), dp(3), dp(3), dp(3))

        val handleView = TextView(context).apply {
            text = "≡"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(22), LayoutParams.MATCH_PARENT)
        }
        addView(handleView)

        slotsContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
            visibility = if (prefs.collapsed) View.GONE else View.VISIBLE
        }
        for (i in 1..5) {
            slotsContainer.addView(SlotView(context, i, prefs))
        }
        addView(slotsContainer)

        attachHandleGestures(handleView)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun attachHandleGestures(handle: TextView) {
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                val collapsed = !prefs.collapsed
                prefs.collapsed = collapsed
                slotsContainer.visibility = if (collapsed) View.GONE else View.VISIBLE
                return true
            }
        })

        var downX = 0f
        var downY = 0f
        handle.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    onDragStart()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - downX).toInt()
                    val dy = (event.rawY - downY).toInt()
                    onDrag(dx, dy)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    onDragEnd()
                    true
                }
                else -> false
            }
        }
    }
}
