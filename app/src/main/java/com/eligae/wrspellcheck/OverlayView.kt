package com.eligae.wrspellcheck

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class OverlayView(context: Context) : LinearLayout(context) {

    init {
        orientation = HORIZONTAL
        setBackgroundColor(Color.parseColor("#CC000000"))
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        setPadding(dp(4), dp(4), dp(4), dp(4))

        // 손잡이 (PR 6에서 드래그 구현)
        addView(TextView(context).apply {
            text = "≡"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(28), LayoutParams.MATCH_PARENT)
        })

        // 슬롯 5개 stack
        val slots = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT
            )
        }
        for (i in 1..5) {
            slots.addView(SlotView(context, i))
        }
        addView(slots)
    }
}
