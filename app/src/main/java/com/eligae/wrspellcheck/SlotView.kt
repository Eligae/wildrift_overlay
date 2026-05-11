package com.eligae.wrspellcheck

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

class SlotView(
    context: Context,
    slotIndex: Int,
    private val prefs: OverlayPrefs,
) : LinearLayout(context) {

    private var state = prefs.loadSlot(slotIndex)
    private val laneLabel: TextView
    private val spell1Button: TextView
    private val spell2Button: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val tick = object : Runnable {
        override fun run() {
            autoReset()
            render()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        orientation = HORIZONTAL
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(26)).apply {
            topMargin = dp(1)
        }

        laneLabel = TextView(context).apply {
            text = LANE_LABELS.getOrElse(slotIndex - 1) { "?" }
            setTextColor(Color.WHITE)
            textSize = 9f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(dp(22), LayoutParams.MATCH_PARENT)
        }
        addView(laneLabel)

        spell1Button = button().also { addView(it) }
        spell2Button = button().also { addView(it) }

        spell1Button.setOnClickListener { toggle(SlotButton.SPELL_1) }
        spell1Button.setOnLongClickListener { cycleSpell(SlotButton.SPELL_1); true }
        spell2Button.setOnClickListener { toggle(SlotButton.SPELL_2) }
        spell2Button.setOnLongClickListener { cycleSpell(SlotButton.SPELL_2); true }

        render()
        handler.post(tick)
    }

    override fun onDetachedFromWindow() {
        handler.removeCallbacks(tick)
        super.onDetachedFromWindow()
    }

    fun setLaneLabelVisible(visible: Boolean) {
        laneLabel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun toggle(which: SlotButton) {
        val now = System.currentTimeMillis()
        state = when (which) {
            SlotButton.SPELL_1 -> state.copy(
                spell1ReadyAtEpochMs = if (state.spell1ReadyAtEpochMs == null)
                    now + state.spell1.defaultCooldownSec * 1000L else null
            )
            SlotButton.SPELL_2 -> state.copy(
                spell2ReadyAtEpochMs = if (state.spell2ReadyAtEpochMs == null)
                    now + state.spell2.defaultCooldownSec * 1000L else null
            )
        }
        prefs.saveSlot(state)
        render()
    }

    private fun cycleSpell(which: SlotButton) {
        state = when (which) {
            SlotButton.SPELL_1 -> state.copy(spell1 = state.spell1.next())
            SlotButton.SPELL_2 -> state.copy(spell2 = state.spell2.next())
        }
        prefs.saveSlot(state)
        render()
    }

    private fun autoReset() {
        val now = System.currentTimeMillis()
        var s = state
        if (s.spell1ReadyAtEpochMs != null && s.spell1ReadyAtEpochMs <= now) {
            s = s.copy(spell1ReadyAtEpochMs = null)
        }
        if (s.spell2ReadyAtEpochMs != null && s.spell2ReadyAtEpochMs <= now) {
            s = s.copy(spell2ReadyAtEpochMs = null)
        }
        state = s
    }

    private fun render() {
        renderButton(spell1Button, state.spell1.label, state.spell1ReadyAtEpochMs)
        renderButton(spell2Button, state.spell2.label, state.spell2ReadyAtEpochMs)
    }

    private fun renderButton(btn: TextView, idleLabel: String, readyAt: Long?) {
        if (readyAt == null) {
            btn.text = idleLabel
            btn.setBackgroundColor(COLOR_IDLE)
            return
        }
        val now = System.currentTimeMillis()
        val remaining = ((readyAt - now + 999) / 1000).toInt().coerceAtLeast(0)
        btn.text = remaining.toString()
        btn.setBackgroundColor(
            when {
                remaining > 10 -> COLOR_COUNTDOWN
                remaining > 0 -> if (remaining % 2 == 0) COLOR_IMMINENT_A else COLOR_IMMINENT_B
                else -> COLOR_IDLE
            }
        )
    }

    private fun button(): TextView {
        val dp = { v: Int -> (v * resources.displayMetrics.density).toInt() }
        return TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11f
            gravity = Gravity.CENTER
            isClickable = true
            isLongClickable = true
            layoutParams = LayoutParams(dp(28), dp(22)).apply {
                marginStart = dp(3)
                gravity = Gravity.CENTER_VERTICAL
            }
        }
    }

    private enum class SlotButton { SPELL_1, SPELL_2 }

    companion object {
        private val LANE_LABELS = arrayOf("TOP", "JUG", "MID", "ADC", "SUP")
        private val COLOR_IDLE = Color.parseColor("#444444")
        private val COLOR_COUNTDOWN = Color.parseColor("#3355AA")
        private val COLOR_IMMINENT_A = Color.parseColor("#AA3300")
        private val COLOR_IMMINENT_B = Color.parseColor("#FF6633")
    }
}
