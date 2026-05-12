package com.eligae.wildrift.overlay.model

import androidx.annotation.DrawableRes
import com.eligae.wildrift.overlay.R

enum class Spell(
    val label: String,
    val defaultCooldownSec: Int,
    /** UI 표시용 (raw Data Dragon, 정상 방향). */
    @DrawableRes val iconRes: Int,
    /**
     * 매칭용 reference — 풀로딩 카드 안 스펠 외형(stylized·원형 마스크·glow 적용).
     * 풀로딩 PNG에서 추출한 ref가 있는 스펠만 별도 _ref drawable 지정.
     * 미지정 시 iconRes를 그대로 사용 (raw Data Dragon — 매칭 정확도는 낮지만 fallback).
     */
    @DrawableRes val refIconRes: Int = iconRes,
) {
    FLASH("F", 150, R.drawable.spell_flash, R.drawable.spell_flash_ref),
    IGNITE("I", 90, R.drawable.spell_ignite, R.drawable.spell_ignite_ref),
    HEAL("H", 120, R.drawable.spell_heal),
    GHOST("G", 120, R.drawable.spell_ghost),
    EXHAUST("E", 120, R.drawable.spell_exhaust),
    BARRIER("B", 120, R.drawable.spell_barrier),
    SMITE("S", 15, R.drawable.spell_smite);

    fun next(): Spell = entries[(ordinal + 1) % entries.size]
}
