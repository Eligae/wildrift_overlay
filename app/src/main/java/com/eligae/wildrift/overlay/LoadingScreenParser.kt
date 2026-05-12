package com.eligae.wildrift.overlay

/**
 * 와일드리프트 로딩 화면 OCR 결과에서 챔피언 5+5명 추출.
 *
 * parseTeams: textBlock 좌표(중심 x/y)로 적팀/아군 분리. ML Kit InputImage.fromBitmap(bitmap, 90)
 * 결과 좌표는 회전된 frame 기준이라 적팀이 위(y < mid), 아군이 아래(y >= mid)로 분리.
 * 같은 팀 안에서는 x 좌표 좌→우 = TOP / JUG / MID / ADC / SUP.
 */
object LoadingScreenParser {

    data class TextLoc(val text: String, val centerX: Float, val centerY: Float)
    data class Teams(val enemies: List<String>, val allies: List<String>)

    fun parseTeams(blocks: List<TextLoc>, rotatedFrameHeight: Int): Teams {
        val picks = mutableListOf<Triple<String, Float, Float>>()
        for (b in blocks) {
            val text = b.text.replace("\n", " ")
            for (name in ChampionRegistry.KNOWN_NAMES) {
                if (text.contains(name) && picks.none { it.first == name }) {
                    picks.add(Triple(name, b.centerX, b.centerY))
                    break
                }
            }
        }
        val mid = rotatedFrameHeight / 2f
        val enemies = picks.filter { it.third < mid }
            .sortedBy { it.second }
            .map { it.first }
            .take(5)
        val allies = picks.filter { it.third >= mid }
            .sortedBy { it.second }
            .map { it.first }
            .take(5)
        return Teams(enemies, allies)
    }

    /** 좌표 정보 없는 단순 추출 (fallback). */
    fun parse(blocks: List<String>): List<String> {
        val found = mutableListOf<String>()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            for (name in ChampionRegistry.KNOWN_NAMES) {
                if (text.contains(name) && !found.contains(name)) {
                    found.add(name)
                }
            }
        }
        return found
    }
}
