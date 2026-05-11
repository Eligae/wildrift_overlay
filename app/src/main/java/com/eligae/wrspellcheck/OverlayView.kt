package com.eligae.wrspellcheck

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView

class OverlayView(context: Context) : FrameLayout(context) {

    init {
        setBackgroundColor(Color.parseColor("#CC000000"))
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        minimumWidth = dp(200)
        minimumHeight = dp(60)
        setPadding(dp(16), dp(8), dp(16), dp(8))

        val label = TextView(context).apply {
            text = "WR"
            setTextColor(Color.WHITE)
            textSize = 16f
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        addView(label)
    }
}
