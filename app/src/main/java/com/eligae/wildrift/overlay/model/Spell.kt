package com.eligae.wildrift.overlay.model

import androidx.annotation.DrawableRes
import com.eligae.wildrift.overlay.R

enum class Spell(
    val label: String,
    val defaultCooldownSec: Int,
    @DrawableRes val iconRes: Int,
) {
    FLASH("F", 150, R.drawable.spell_flash),
    IGNITE("I", 90, R.drawable.spell_ignite),
    HEAL("H", 120, R.drawable.spell_heal),
    GHOST("G", 120, R.drawable.spell_ghost),
    EXHAUST("E", 120, R.drawable.spell_exhaust),
    BARRIER("B", 120, R.drawable.spell_barrier),
    SMITE("S", 15, R.drawable.spell_smite);

    fun next(): Spell = entries[(ordinal + 1) % entries.size]
}
