package com.eligae.wildrift.overlay.floating

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import coil.load
import com.eligae.wildrift.overlay.api.ChampionsCache
import com.eligae.wildrift.overlay.model.Spell
import com.eligae.wildrift.overlay.model.SlotState
import com.eligae.wildrift.overlay.prefs.OverlayPrefs

class SlotView(
    context: Context,
    val slotIndex: Int,
    private val prefs: OverlayPrefs,
) : LinearLayout(context) {

    private var state = prefs.loadSlot(slotIndex)
    private val laneLabel: TextView
    private val championIcon: ImageView
    private val spell1Button: TextView
    private val spell2Button: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val scale = prefs.scale
    private val championsCache = ChampionsCache(context.applicationContext)

    private val tick = object : Runnable {
        override fun run() {
            autoReset()
            render()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        orientation = HORIZONTAL
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, dp(26)).apply {
            topMargin = dp(1)
        }

        val labelContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(dp(34), LayoutParams.MATCH_PARENT)
        }
        laneLabel = TextView(context).apply {
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        championIcon = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        labelContainer.addView(laneLabel)
        labelContainer.addView(championIcon)
        applyLabel()
        addView(labelContainer)

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

    /** prefs 갱신 후 호출 (broadcast 받았을 때). */
    fun reload() {
        state = prefs.loadSlot(slotIndex)
        applyLabel()
        render()
    }

    private fun applyLabel() {
        val champ = state.championName
        if (champ != null) {
            val avatar = championsCache.avatarFor(champ)
            if (avatar != null) {
                championIcon.load(avatar) { crossfade(true) }
                championIcon.visibility = View.VISIBLE
                laneLabel.visibility = View.GONE
                return
            }
            // 캐시 미스/매핑 실패 — 텍스트 fallback
            laneLabel.text = champ
            laneLabel.textSize = 8f * scale
            laneLabel.visibility = View.VISIBLE
            championIcon.visibility = View.GONE
        } else {
            laneLabel.text = LANE_LABELS.getOrElse(slotIndex - 1) { "?" }
            laneLabel.textSize = 9f * scale
            laneLabel.visibility = View.VISIBLE
            championIcon.visibility = View.GONE
        }
    }

    private fun dp(v: Int): Int = (v * scale * resources.displayMetrics.density).toInt()

    private fun toggle(which: SlotButton) {
        val now = System.currentTimeMillis()
        state = when (which) {
            SlotButton.SPELL_1 -> {
                val s = state.spell1 ?: Spell.FLASH
                state.copy(
                    spell1 = s,
                    spell1ReadyAtEpochMs = if (state.spell1ReadyAtEpochMs == null)
                        now + s.defaultCooldownSec * 1000L else null,
                )
            }
            SlotButton.SPELL_2 -> {
                val s = state.spell2 ?: Spell.IGNITE
                state.copy(
                    spell2 = s,
                    spell2ReadyAtEpochMs = if (state.spell2ReadyAtEpochMs == null)
                        now + s.defaultCooldownSec * 1000L else null,
                )
            }
        }
        prefs.saveSlot(state)
        render()
    }

    private fun cycleSpell(which: SlotButton) {
        state = when (which) {
            SlotButton.SPELL_1 -> state.copy(spell1 = (state.spell1 ?: Spell.FLASH).next())
            SlotButton.SPELL_2 -> state.copy(spell2 = (state.spell2 ?: Spell.IGNITE).next())
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
        renderButton(spell1Button, state.spell1, state.spell1ReadyAtEpochMs)
        renderButton(spell2Button, state.spell2, state.spell2ReadyAtEpochMs)
    }

    private fun renderButton(btn: TextView, spell: Spell?, readyAt: Long?) {
        if (spell == null) {
            // 미감지 — 배경 없이 "?" 표시. 사용자가 long-press 해서 수동 설정 유도.
            btn.background = null
            btn.text = "?"
            btn.setTextColor(Color.parseColor("#A8A194"))
            return
        }
        btn.setBackgroundResource(spell.iconRes)
        if (readyAt == null) {
            btn.text = ""
            btn.background?.alpha = 255
            return
        }
        val now = System.currentTimeMillis()
        val remaining = ((readyAt - now + 999) / 1000).toInt().coerceAtLeast(0)
        btn.text = remaining.toString()
        btn.background?.alpha = 100
        btn.setTextColor(if (remaining > 10) Color.WHITE else Color.parseColor("#FF6633"))
    }

    private fun button(): TextView {
        return TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 11f * scale
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
