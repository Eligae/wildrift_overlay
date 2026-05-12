package com.eligae.wildrift.overlay.parse

import com.eligae.wildrift.overlay.model.Spell

object ChatParser {

    data class Match(val champion: String, val spell: Spell)

    // OCR 오인식 보정 포함. 점/정, 막/막 등 비슷한 글자 변형.
    private val spellAliases: Map<String, Spell> = mapOf(
        "점멸" to Spell.FLASH,
        "정멸" to Spell.FLASH,
        "점화" to Spell.IGNITE,
        "정화" to Spell.IGNITE,
        "회복" to Spell.HEAL,
        "회독" to Spell.HEAL,
        "유체화" to Spell.GHOST,
        "유체" to Spell.GHOST,
        "탈진" to Spell.EXHAUST,
        "탈전" to Spell.EXHAUST,
        "방어막" to Spell.BARRIER,
        "방어" to Spell.BARRIER,
        "강타" to Spell.SMITE,
    )

    fun parse(blocks: List<String>, extraKnownNames: Set<String> = emptySet()): List<Match> {
        val results = mutableListOf<Match>()
        val allNames: List<String> = (ChampionRegistry.KNOWN_NAMES + extraKnownNames).distinct().toList()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            val spellEntry = spellAliases.entries.firstOrNull { text.contains(it.key) } ?: continue
            val spellIdx = text.indexOf(spellEntry.key)
            // 시스템 메시지 형식 "[닉네임] [챔피언] - [스펠]" 가정.
            // 스펠 앞쪽에서 가장 마지막에 등장한 챔피언명을 진짜 챔피언으로 본다.
            val pre = text.substring(0, spellIdx)
            val champ = allNames
                .mapNotNull { name ->
                    val idx = pre.lastIndexOf(name)
                    if (idx >= 0) name to idx else null
                }
                .maxByOrNull { it.second }
                ?.first ?: continue
            results.add(Match(ChampionRegistry.canonical(champ), spellEntry.value))
        }
        return results
    }
}
