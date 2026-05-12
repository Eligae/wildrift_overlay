package com.eligae.wildrift.overlay

/**
 * 와일드리프트 로딩 화면 OCR 결과에서 챔피언 5+5명 추출.
 *
 * 두 단계 anchor 전략:
 *   1) 픽 직후 우리 팀만 보여주는 5명 화면 → ally anchor로 저장 (caller가 picks == 5명일 때 호출).
 *   2) 10명 로딩 화면 → anchor가 있으면 anchor 5명을 동맹으로, 나머지가 적팀.
 *      anchor가 없으면 y 좌표(회전된 frame 기준)로 적팀(위쪽)/동맹(아래쪽) 분리 — fallback.
 *   같은 row 안에서는 sortedBy { centerX } = 좌→우 = TOP→SUP (사용자 확정, 회전 후 좌표축은 다음 게임에서 검증).
 */
object LoadingScreenParser {

    data class TextLoc(val text: String, val centerX: Float, val centerY: Float)
    data class Pick(val canonical: String, val centerX: Float, val centerY: Float)
    data class Teams(val enemies: List<String>, val allies: List<String>, val picks: List<Pick>)

    fun parseTeams(
        blocks: List<TextLoc>,
        rotatedFrameHeight: Int,
        allyAnchor: List<String>? = null,
    ): Teams {
        val picks = extractPicks(blocks)
        if (allyAnchor != null && allyAnchor.isNotEmpty()) {
            val anchorSet = allyAnchor.toSet()
            val allies = picks.filter { it.canonical in anchorSet }
                .sortedBy { it.centerX }
                .map { it.canonical }
            val enemies = picks.filter { it.canonical !in anchorSet }
                .sortedBy { it.centerX }
                .map { it.canonical }
                .take(5)
            return Teams(enemies, allies, picks)
        }
        // fallback — y로 분리
        val mid = rotatedFrameHeight / 2f
        val enemies = picks.filter { it.centerY < mid }
            .sortedBy { it.centerX }
            .map { it.canonical }
            .take(5)
        val allies = picks.filter { it.centerY >= mid }
            .sortedBy { it.centerX }
            .map { it.canonical }
            .take(5)
        return Teams(enemies, allies, picks)
    }

    private fun extractPicks(blocks: List<TextLoc>): List<Pick> {
        val picks = mutableListOf<Pick>()
        for (b in blocks) {
            val text = b.text.replace("\n", " ")
            for (name in ChampionRegistry.KNOWN_NAMES) {
                if (text.contains(name)) {
                    val canon = ChampionRegistry.canonical(name)
                    if (picks.none { it.canonical == canon }) {
                        picks.add(Pick(canon, b.centerX, b.centerY))
                    }
                    break
                }
            }
        }
        return picks
    }

    /** 좌표 정보 없는 단순 추출 (fallback). */
    fun parse(blocks: List<String>): List<String> {
        val found = mutableListOf<String>()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            for (name in ChampionRegistry.KNOWN_NAMES) {
                if (text.contains(name)) {
                    val canon = ChampionRegistry.canonical(name)
                    if (!found.contains(canon)) found.add(canon)
                }
            }
        }
        return found
    }
}
