package com.eligae.wildrift.overlay

import android.content.Context

class OverlayPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("overlay_state", Context.MODE_PRIVATE)

    var overlayX: Int
        get() = prefs.getInt("overlay_x", 32)
        set(value) = prefs.edit().putInt("overlay_x", value).apply()

    var overlayY: Int
        get() = prefs.getInt("overlay_y", 200)
        set(value) = prefs.edit().putInt("overlay_y", value).apply()

    var collapseLevel: Int
        get() = prefs.getInt("collapse_level", 0)
        set(value) = prefs.edit().putInt("collapse_level", value.coerceIn(0, 2)).apply()

    var scale: Float
        get() = prefs.getFloat("scale", 1.0f)
        set(value) = prefs.edit().putFloat("scale", value.coerceIn(0.5f, 1.5f)).apply()

    fun loadSlot(index: Int): SlotState {
        val k = "slot_$index"
        return SlotState(
            index = index,
            championName = prefs.getString("${k}_champion", null)?.takeIf { it.isNotBlank() },
            spell1 = Spell.entries.getOrElse(prefs.getInt("${k}_spell1", 0)) { Spell.FLASH },
            spell2 = Spell.entries.getOrElse(prefs.getInt("${k}_spell2", 1)) { Spell.IGNITE },
            spell1ReadyAtEpochMs = prefs.getLong("${k}_spell1_ready", -1L).takeIf { it > 0 },
            spell2ReadyAtEpochMs = prefs.getLong("${k}_spell2_ready", -1L).takeIf { it > 0 },
        )
    }

    fun saveSlot(state: SlotState) {
        val k = "slot_${state.index}"
        prefs.edit().apply {
            putString("${k}_champion", state.championName ?: "")
            putInt("${k}_spell1", state.spell1.ordinal)
            putInt("${k}_spell2", state.spell2.ordinal)
            putLong("${k}_spell1_ready", state.spell1ReadyAtEpochMs ?: -1L)
            putLong("${k}_spell2_ready", state.spell2ReadyAtEpochMs ?: -1L)
        }.apply()
    }

    /** 챔피언명만 별도 갱신 (broadcast에서 호출). */
    fun setSlotChampion(index: Int, name: String?) {
        prefs.edit().putString("slot_${index}_champion", name ?: "").apply()
    }
}
