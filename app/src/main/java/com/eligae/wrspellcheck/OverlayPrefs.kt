package com.eligae.wrspellcheck

import android.content.Context

class OverlayPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("overlay_state", Context.MODE_PRIVATE)

    var overlayX: Int
        get() = prefs.getInt("overlay_x", 32)
        set(value) = prefs.edit().putInt("overlay_x", value).apply()

    var overlayY: Int
        get() = prefs.getInt("overlay_y", 200)
        set(value) = prefs.edit().putInt("overlay_y", value).apply()

    var collapsed: Boolean
        get() = prefs.getBoolean("collapsed", false)
        set(value) = prefs.edit().putBoolean("collapsed", value).apply()

    fun loadSlot(index: Int): SlotState {
        val k = "slot_$index"
        return SlotState(
            index = index,
            spell1 = Spell.entries.getOrElse(prefs.getInt("${k}_spell1", 0)) { Spell.FLASH },
            spell2 = Spell.entries.getOrElse(prefs.getInt("${k}_spell2", 1)) { Spell.IGNITE },
            ultimateCooldownSec = prefs.getInt("${k}_ult_cd", 60),
            spell1ReadyAtEpochMs = prefs.getLong("${k}_spell1_ready", -1L).takeIf { it > 0 },
            spell2ReadyAtEpochMs = prefs.getLong("${k}_spell2_ready", -1L).takeIf { it > 0 },
            ultimateReadyAtEpochMs = prefs.getLong("${k}_ult_ready", -1L).takeIf { it > 0 },
        )
    }

    fun saveSlot(state: SlotState) {
        val k = "slot_${state.index}"
        prefs.edit().apply {
            putInt("${k}_spell1", state.spell1.ordinal)
            putInt("${k}_spell2", state.spell2.ordinal)
            putInt("${k}_ult_cd", state.ultimateCooldownSec)
            putLong("${k}_spell1_ready", state.spell1ReadyAtEpochMs ?: -1L)
            putLong("${k}_spell2_ready", state.spell2ReadyAtEpochMs ?: -1L)
            putLong("${k}_ult_ready", state.ultimateReadyAtEpochMs ?: -1L)
        }.apply()
    }
}
