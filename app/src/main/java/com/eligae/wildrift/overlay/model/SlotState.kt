package com.eligae.wildrift.overlay.model

data class SlotState(
    val index: Int,
    val championName: String? = null,
    val spell1: Spell? = null,
    val spell2: Spell? = null,
    val spell1ReadyAtEpochMs: Long? = null,
    val spell2ReadyAtEpochMs: Long? = null,
)
