package com.eligae.wildrift.overlay

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

    fun parse(blocks: List<String>): List<Match> {
        val results = mutableListOf<Match>()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            val champ = ChampionRegistry.KNOWN_NAMES.firstOrNull { text.contains(it) } ?: continue
            val spell = spellAliases.entries.firstOrNull { text.contains(it.key) }?.value ?: continue
            results.add(Match(champ, spell))
        }
        return results
    }
}
