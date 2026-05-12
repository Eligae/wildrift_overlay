package com.eligae.wildrift.overlay.parse

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
        extraKnownNames: Set<String> = emptySet(),
    ): Teams {
        val picks = extractPicks(blocks, extraKnownNames)
        if (allyAnchor != null && allyAnchor.isNotEmpty()) {
            val anchorSet = allyAnchor.toSet()
            // anchor 흐름: anchor 5명을 동맹, 나머지가 적팀.
            // 한 column(team) 안에서 라인 순서는 centerY (회전 frame에서 y 작은 게 TOP).
            val allies = picks.filter { it.canonical in anchorSet }
                .sortedBy { it.centerY }
                .map { it.canonical }
            val enemies = picks.filter { it.canonical !in anchorSet }
                .sortedBy { it.centerY }
                .map { it.canonical }
                .take(5)
            return Teams(enemies, allies, picks)
        }
        // fallback — picks를 x로 두 column 자동 분리 (회전 frame에서 column이 팀).
        // x<midX 한 팀, x>=midX 다른 팀. 어느 쪽이 적팀인지는 사용자 캘리브레이션 또는 향후 anchor 결정.
        // PoC: x 작은 column을 적팀으로 가정 (이전 게임 데이터로 확인됨).
        // 라인 순서: 한 column 내 y 작은 게 TOP.
        if (picks.size < 6) {
            return Teams(emptyList(), emptyList(), picks)
        }
        val xs = picks.map { it.centerX }
        val midX = (xs.min() + xs.max()) / 2f
        val enemies = picks.filter { it.centerX < midX }
            .sortedBy { it.centerY }
            .map { it.canonical }
            .take(5)
        val allies = picks.filter { it.centerX >= midX }
            .sortedBy { it.centerY }
            .map { it.canonical }
            .take(5)
        return Teams(enemies, allies, picks)
    }

    private fun extractPicks(blocks: List<TextLoc>, extra: Set<String>): List<Pick> {
        val picks = mutableListOf<Pick>()
        val allNames: Sequence<String> = (ChampionRegistry.KNOWN_NAMES.asSequence() + extra.asSequence()).distinct()
        for (b in blocks) {
            val text = b.text.replace("\n", " ")
            for (name in allNames) {
                if (name.isBlank()) continue
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
    fun parse(blocks: List<String>, extraKnownNames: Set<String> = emptySet()): List<String> {
        val found = mutableListOf<String>()
        val allNames: Sequence<String> = (ChampionRegistry.KNOWN_NAMES.asSequence() + extraKnownNames.asSequence()).distinct()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            for (name in allNames) {
                if (name.isBlank()) continue
                if (text.contains(name)) {
                    val canon = ChampionRegistry.canonical(name)
                    if (!found.contains(canon)) found.add(canon)
                }
            }
        }
        return found
    }
}
