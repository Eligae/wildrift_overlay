package com.eligae.wrspellcheck

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class SlotView(context: Context, private val slotIndex: Int) : LinearLayout(context) {

    init {
        orientation = HORIZONTAL
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(36)).apply {
            topMargin = dp(2)
        }

        addView(TextView(context).apply {
            text = "P$slotIndex"
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(24), LayoutParams.MATCH_PARENT)
        })

        addView(button("F"))   // 점멸
        addView(button("I"))   // 점화
        addView(button("R"))   // 궁
    }

    private fun button(label: String): TextView {
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        return TextView(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 13f
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#444444"))
            isClickable = true
            isLongClickable = true
            layoutParams = LayoutParams(dp(36), dp(32)).apply {
                marginStart = dp(4)
                gravity = Gravity.CENTER_VERTICAL
            }
            // 탭/롱프레스 동작은 PR 5에서.
        }
    }
}
