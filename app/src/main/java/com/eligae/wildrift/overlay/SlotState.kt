package com.eligae.wildrift.overlay

data class SlotState(
    val index: Int,
    val spell1: Spell = Spell.FLASH,
    val spell2: Spell = Spell.IGNITE,
    val spell1ReadyAtEpochMs: Long? = null,
    val spell2ReadyAtEpochMs: Long? = null,
)
