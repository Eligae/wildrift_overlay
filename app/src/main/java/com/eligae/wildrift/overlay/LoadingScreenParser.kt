package com.eligae.wildrift.overlay

/**
 * 와일드리프트 로딩 화면 OCR 결과에서 챔피언 5+5명 추출.
 *
 * 적팀/아군 구분은 좌표 정보 필요 — PoC는 모든 매치 반환 (적팀 식별은 v2).
 * 사용자는 로딩 화면 캡처 트리거 후 logcat에서 LOADING 로그 확인.
 */
object LoadingScreenParser {

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
