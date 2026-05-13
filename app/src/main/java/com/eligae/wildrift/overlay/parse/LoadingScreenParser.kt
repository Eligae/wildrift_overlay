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
    /**
     * enemies/allies는 항상 5개 슬롯. 라인 순서 = [TOP, JUG, MID, ADC, SUP].
     * OCR 누락 시 해당 슬롯이 null — 위치 정확성 우선.
     */
    data class Teams(val enemies: List<String?>, val allies: List<String?>, val picks: List<Pick>)

    /** 풀로딩 카드 5개의 y 비율 (rotated frame height 기준 0~1). */
    private val LANE_Y_RATIOS = listOf(0.234f, 0.365f, 0.496f, 0.626f, 0.757f)

    fun parseTeams(
        blocks: List<TextLoc>,
        rotatedFrameHeight: Int,
        allyAnchor: List<String>? = null,
        extraKnownNames: Set<String> = emptySet(),
        skinAliasMap: Map<String, String> = emptyMap(),
    ): Teams {
        val picks = extractPicks(blocks, extraKnownNames, skinAliasMap)
        if (allyAnchor != null && allyAnchor.isNotEmpty()) {
            val anchorSet = allyAnchor.toSet()
            val allyPicks = picks.filter { it.canonical in anchorSet }
            val enemyPicks = picks.filter { it.canonical !in anchorSet }
            return Teams(
                enemies = mapToLaneSlots(enemyPicks, rotatedFrameHeight),
                allies = mapToLaneSlots(allyPicks, rotatedFrameHeight),
                picks = picks,
            )
        }
        // fallback — picks를 x column으로 두 팀 분리.
        if (picks.size < 6) {
            return Teams(List(5) { null }, List(5) { null }, picks)
        }
        val xs = picks.map { it.centerX }
        val midX = (xs.min() + xs.max()) / 2f
        return Teams(
            enemies = mapToLaneSlots(picks.filter { it.centerX < midX }, rotatedFrameHeight),
            allies = mapToLaneSlots(picks.filter { it.centerX >= midX }, rotatedFrameHeight),
            picks = picks,
        )
    }

    /**
     * 한 팀의 picks를 라인 슬롯 5개에 매핑. 각 pick의 centerY 비율을 [LANE_Y_RATIOS]에 가장
     * 가까운 인덱스에 박는다. 매핑된 슬롯이 이미 차 있으면 후순위 pick은 버려진다.
     */
    private fun mapToLaneSlots(col: List<Pick>, rotatedFrameHeight: Int): List<String?> {
        if (rotatedFrameHeight <= 0) return List(5) { null }
        val slots = MutableList<String?>(5) { null }
        for (pick in col) {
            val ratio = pick.centerY / rotatedFrameHeight
            val idx = LANE_Y_RATIOS.indices.minByOrNull {
                kotlin.math.abs(LANE_Y_RATIOS[it] - ratio)
            } ?: continue
            if (slots[idx] == null) slots[idx] = pick.canonical
        }
        return slots
    }

    private fun extractPicks(
        blocks: List<TextLoc>,
        extra: Set<String>,
        skinAliasMap: Map<String, String>,
    ): List<Pick> {
        val picks = mutableListOf<Pick>()
        // 후보 = 정적 KNOWN_NAMES + 동적 챔피언명 + 스킨명. 길이 내림차순으로 짧은 이름이 긴 이름의
        // 부분 fuzzy로 잘못 매칭되는 사고 방지 (예: "신지드" 텍스트에 "진"이 ㅅ↔ㅈ 유사로 매칭).
        val allNames: List<String> = (ChampionRegistry.KNOWN_NAMES + extra + skinAliasMap.keys)
            .distinct()
            .sortedByDescending { it.length }
        val candidates = KoreanFuzzy.Candidates(allNames)
        for (b in blocks) {
            val text = b.text.replace("\n", " ")
            var matched: String? = null
            for (name in allNames) {
                if (name.isBlank()) continue
                if (KoreanFuzzy.fuzzyContains(text, name)) {
                    matched = name
                    break
                }
            }
            if (matched == null) {
                matched = KoreanFuzzy.bestMatch(text, candidates, threshold = 0.7)?.first
            }
            if (matched != null) {
                val canon = skinAliasMap[matched] ?: ChampionRegistry.canonical(matched)
                if (picks.none { it.canonical == canon }) {
                    picks.add(Pick(canon, b.centerX, b.centerY))
                }
            }
        }
        return picks
    }

    /** 좌표 정보 없는 단순 추출 (fallback). */
    fun parse(
        blocks: List<String>,
        extraKnownNames: Set<String> = emptySet(),
        skinAliasMap: Map<String, String> = emptyMap(),
    ): List<String> {
        val found = mutableListOf<String>()
        val allNames: List<String> = (ChampionRegistry.KNOWN_NAMES + extraKnownNames + skinAliasMap.keys)
            .distinct()
            .sortedByDescending { it.length }
        val candidates = KoreanFuzzy.Candidates(allNames)
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            var hit = false
            for (name in allNames) {
                if (name.isBlank()) continue
                if (KoreanFuzzy.fuzzyContains(text, name)) {
                    val canon = skinAliasMap[name] ?: ChampionRegistry.canonical(name)
                    if (!found.contains(canon)) found.add(canon)
                    hit = true
                }
            }
            if (!hit) {
                val best = KoreanFuzzy.bestMatch(text, candidates, threshold = 0.7)?.first
                if (best != null) {
                    val canon = skinAliasMap[best] ?: ChampionRegistry.canonical(best)
                    if (!found.contains(canon)) found.add(canon)
                }
            }
        }
        return found
    }
}
