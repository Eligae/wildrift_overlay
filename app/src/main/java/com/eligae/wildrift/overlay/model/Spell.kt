package com.eligae.wildrift.overlay.model

enum class Spell(val label: String, val defaultCooldownSec: Int) {
    FLASH("F", 150),
    IGNITE("I", 90),
    HEAL("H", 120),
    GHOST("G", 120),
    EXHAUST("E", 120),
    BARRIER("B", 120),
    SMITE("S", 15);

    fun next(): Spell = entries[(ordinal + 1) % entries.size]
}
