package com.eligae.wildrift.overlay.model

data class SlotState(
    val index: Int,
    val championName: String? = null,
    val spell1: Spell = Spell.FLASH,
    val spell2: Spell = Spell.IGNITE,
    val spell1ReadyAtEpochMs: Long? = null,
    val spell2ReadyAtEpochMs: Long? = null,
)
