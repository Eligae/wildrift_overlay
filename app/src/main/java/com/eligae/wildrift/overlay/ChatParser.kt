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

    // PoC 챔피언 한국명 화이트리스트 (자주 보이는 ~50명).
    // server/data/champion-kr.json과 일치시킬 자산. 추후 assets로 옮길 수 있음.
    private val championNames: Set<String> = setOf(
        "가렌", "아트록스", "다리우스", "야스오", "요네", "리븐", "잭스", "케일", "이렐리아",
        "피오라", "카밀", "쉔", "케넨", "트린다미어", "오공", "나르", "말파이트", "사이온",
        "레넥톤", "포피", "뽀삐", "마오카이", "오른", "탐 켄치",
        "리 신", "마스터 이", "신 짜오", "그레이브즈", "킨드레드", "니달리", "에코",
        "카직스", "렝가", "헤카림", "비에고", "아무무", "워윅", "람머스", "릴리아",
        "야스오", "제드", "아칼리", "카타리나", "탈론", "라이즈", "신드라", "오리아나",
        "럭스", "빅토르", "조이", "다이애나", "트위스티드 페이트", "갈리오", "이즈리얼",
        "징크스", "케이틀린", "베인", "애쉬", "미스 포츈", "드레이븐", "코그모", "트위치",
        "칼리스타", "사미라", "제리", "시비르", "트리스타나", "스몰더", "진",
        "쓰레쉬", "레오나", "브라움", "노틸러스", "알리스타", "라칸", "모르가나", "잔나",
        "나미", "소나", "유미", "룰루", "세나", "카르마", "파이크", "블리츠크랭크",
        "노라", "밀리오", "렐", "브랜드", "자이라", "벨코즈", "베이가",
    )

    fun parse(blocks: List<String>): List<Match> {
        val results = mutableListOf<Match>()
        for (raw in blocks) {
            val text = raw.replace("\n", " ")
            val champ = championNames.firstOrNull { text.contains(it) } ?: continue
            val spell = spellAliases.entries.firstOrNull { text.contains(it.key) }?.value ?: continue
            results.add(Match(champ, spell))
        }
        return results
    }
}
