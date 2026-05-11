package com.eligae.wrspellcheck

data class SlotState(
    val index: Int,
    val spell1: Spell = Spell.FLASH,
    val spell2: Spell = Spell.IGNITE,
    val ultimateCooldownSec: Int = 60,
    val spell1ReadyAtEpochMs: Long? = null,
    val spell2ReadyAtEpochMs: Long? = null,
    val ultimateReadyAtEpochMs: Long? = null,
)
