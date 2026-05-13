package com.eligae.wildrift.overlay.parse

/**
 * 한글 자모 분해 + 시각 유사 그룹 기반 fuzzy 매칭.
 * OCR 한글 오인식이 자모 한 자리 차이로 흔히 일어남 — ㅌ↔ㄹ, ㅁ↔ㅇ, ㄴ↔ㄷ 등 (시각 유사).
 * 글자 길이 보존 + 자모 자리별로 정확/유사 매칭. 다른 자모 자리는 정확 일치 요구.
 *
 * 위험 — false positive 가능 (예: "이렐리아" vs "이텔리아"). KNOWN_NAMES 충돌 없으면 OK.
 */
object KoreanFuzzy {

    private const val HANGUL_BASE = 0xAC00
    private const val MEDIAL = 21
    private const val FINAL = 28

    data class Jamo(val initial: Int, val medial: Int, val final: Int)

    /** 초성/종성 인덱스 — 한글 유니코드 표준 순서. */
    // 초성 19개: 0=ㄱ 1=ㄲ 2=ㄴ 3=ㄷ 4=ㄸ 5=ㄹ 6=ㅁ 7=ㅂ 8=ㅃ 9=ㅅ 10=ㅆ 11=ㅇ 12=ㅈ 13=ㅉ 14=ㅊ 15=ㅋ 16=ㅌ 17=ㅍ 18=ㅎ
    private val SIMILAR_INITIAL: Map<Int, Set<Int>> = buildMap {
        // ㄹ ↔ ㅌ — OCR이 가로획 갯수 헷갈림
        put(5, setOf(16))
        put(16, setOf(5))
        // ㅁ ↔ ㅇ — 둥글기와 사각형 헷갈림
        put(6, setOf(11))
        put(11, setOf(6))
        // ㄴ ↔ ㄷ
        put(2, setOf(3))
        put(3, setOf(2))
        // ㄱ ↔ ㅋ
        put(0, setOf(15))
        put(15, setOf(0))
        // ㅅ ↔ ㅈ ↔ ㅊ
        put(9, setOf(12, 14))
        put(12, setOf(9, 14))
        put(14, setOf(9, 12))
    }

    // 종성 28개 (0=없음). 위 초성 매핑과 인덱스 다름 — 종성용 별도.
    // 종성 자모: 0=∅ 1=ㄱ 2=ㄲ 3=ㄳ 4=ㄴ 5=ㄵ 6=ㄶ 7=ㄷ 8=ㄹ 9=ㄺ 10=ㄻ 11=ㄼ 12=ㄽ 13=ㄾ 14=ㄿ 15=ㅀ 16=ㅁ 17=ㅂ 18=ㅄ 19=ㅅ 20=ㅆ 21=ㅇ 22=ㅈ 23=ㅊ 24=ㅋ 25=ㅌ 26=ㅍ 27=ㅎ
    private val SIMILAR_FINAL: Map<Int, Set<Int>> = buildMap {
        // ㄹ ↔ ㅌ
        put(8, setOf(25))
        put(25, setOf(8))
        // ㅁ ↔ ㅇ
        put(16, setOf(21))
        put(21, setOf(16))
        // ㄴ ↔ ㄷ
        put(4, setOf(7))
        put(7, setOf(4))
        // ㄱ ↔ ㅋ
        put(1, setOf(24))
        put(24, setOf(1))
    }

    fun decompose(c: Char): Jamo? {
        val code = c.code - HANGUL_BASE
        if (code < 0 || code >= 19 * MEDIAL * FINAL) return null
        return Jamo(
            initial = code / (MEDIAL * FINAL),
            medial = (code % (MEDIAL * FINAL)) / FINAL,
            final = code % FINAL,
        )
    }

    private fun similarChar(a: Char, b: Char): Boolean {
        if (a == b) return true
        val ja = decompose(a) ?: return false
        val jb = decompose(b) ?: return false
        val initOk = ja.initial == jb.initial ||
            SIMILAR_INITIAL[ja.initial]?.contains(jb.initial) == true
        val medialOk = ja.medial == jb.medial
        val finalOk = ja.final == jb.final ||
            SIMILAR_FINAL[ja.final]?.contains(jb.final) == true
        return initOk && medialOk && finalOk
    }

    /**
     * text가 name을 fuzzy로 포함하는지. 모든 글자가 정확 일치 또는 자모 한 자리 시각 유사면 true.
     */
    fun fuzzyContains(text: String, name: String): Boolean {
        if (name.isEmpty()) return false
        if (text.contains(name)) return true
        if (text.length < name.length) return false
        for (i in 0..(text.length - name.length)) {
            var ok = true
            for (j in name.indices) {
                if (!similarChar(text[i + j], name[j])) {
                    ok = false
                    break
                }
            }
            if (ok) return true
        }
        return false
    }

    /**
     * fuzzy lastIndexOf — text 뒤쪽부터 name이 포함되는 시작 인덱스.
     */
    fun fuzzyLastIndexOf(text: String, name: String): Int {
        if (name.isEmpty() || text.length < name.length) return -1
        for (i in (text.length - name.length) downTo 0) {
            var ok = true
            for (j in name.indices) {
                if (!similarChar(text[i + j], name[j])) {
                    ok = false
                    break
                }
            }
            if (ok) return i
        }
        return -1
    }

    // --- 자모 단위 가중 Levenshtein 기반 최근접 매칭 -------------------

    /** 자모 토큰: 초성/중성/종성·기타 문자를 구분된 int로 인코딩. */
    private const val MEDIAL_BASE = 100
    private const val FINAL_BASE = 200
    private const val OTHER_BASE = 1000

    /** 한글은 자모 단위로, 비한글은 char 단위로 token 시퀀스 생성. */
    private fun tokenize(s: String): IntArray {
        val out = ArrayList<Int>(s.length * 3)
        for (c in s) {
            val j = decompose(c)
            if (j != null) {
                out.add(j.initial)
                out.add(MEDIAL_BASE + j.medial)
                if (j.final != 0) out.add(FINAL_BASE + j.final)
            } else if (c == ' ' || c == '\t') {
                // 공백은 무시 — OCR이 단어 사이 공백을 일관성 없이 넣음.
            } else {
                out.add(OTHER_BASE + c.code)
            }
        }
        return IntArray(out.size) { out[it] }
    }

    /** subst 비용. 시각 유사 자모 0.5, 그 외 1.0. */
    private fun substCost(a: Int, b: Int): Double {
        if (a == b) return 0.0
        // 초성 영역 (0..18)
        if (a in 0..18 && b in 0..18) {
            if (SIMILAR_INITIAL[a]?.contains(b) == true) return 0.5
            return 1.0
        }
        // 종성 영역 (FINAL_BASE..)
        if (a >= FINAL_BASE && a < OTHER_BASE && b >= FINAL_BASE && b < OTHER_BASE) {
            val fa = a - FINAL_BASE
            val fb = b - FINAL_BASE
            if (SIMILAR_FINAL[fa]?.contains(fb) == true) return 0.5
            return 1.0
        }
        return 1.0
    }

    /** 두 토큰 시퀀스의 가중 Levenshtein. */
    private fun weightedDistance(a: IntArray, b: IntArray): Double {
        val n = a.size; val m = b.size
        if (n == 0) return m.toDouble()
        if (m == 0) return n.toDouble()
        var prev = DoubleArray(m + 1) { it.toDouble() }
        var curr = DoubleArray(m + 1)
        for (i in 1..n) {
            curr[0] = i.toDouble()
            for (j in 1..m) {
                val cost = substCost(a[i - 1], b[j - 1])
                curr[j] = minOf(
                    prev[j] + 1.0,        // deletion
                    curr[j - 1] + 1.0,    // insertion
                    prev[j - 1] + cost,   // substitution
                )
            }
            val tmp = prev; prev = curr; curr = tmp
        }
        return prev[m]
    }

    /**
     * text 안에서 name과 가장 유사한 substring 점수(0~1).
     * name 토큰 길이 ±Δ 윈도우를 슬라이드. 윈도우 없으면 전체 거리.
     */
    private fun substringSimilarity(textTokens: IntArray, nameTokens: IntArray): Double {
        val n = nameTokens.size
        if (n == 0) return 0.0
        val t = textTokens.size
        if (t == 0) return 0.0
        if (t <= n + 2) {
            // 짧은 텍스트 — 전체 비교
            val d = weightedDistance(textTokens, nameTokens)
            return 1.0 - d / maxOf(n, t)
        }
        val delta = 2
        var best = 0.0
        for (w in maxOf(1, n - delta)..(n + delta)) {
            if (w > t) break
            for (s in 0..(t - w)) {
                val sub = textTokens.copyOfRange(s, s + w)
                val d = weightedDistance(sub, nameTokens)
                val sim = 1.0 - d / maxOf(n, w)
                if (sim > best) best = sim
                if (best >= 1.0) return 1.0
            }
        }
        return best
    }

    /** 후보 미리 토큰화 결과 — 핫 패스에서 재사용. */
    class Candidates(names: Collection<String>) {
        val list: List<String> = names.distinct()
        val tokens: List<IntArray> = list.map { tokenize(it) }
    }

    /**
     * text와 가장 유사한 후보 + 점수(0~1)를 반환. threshold 미달이면 null.
     * candidates는 [Candidates]로 미리 토큰화하면 호출당 비용 절감.
     */
    fun bestMatch(
        text: String,
        candidates: Candidates,
        threshold: Double = 0.7,
    ): Pair<String, Double>? {
        if (text.isEmpty() || candidates.list.isEmpty()) return null
        val tt = tokenize(text)
        var bestName: String? = null
        var bestScore = 0.0
        for (i in candidates.list.indices) {
            val sim = substringSimilarity(tt, candidates.tokens[i])
            if (sim > bestScore) {
                bestScore = sim
                bestName = candidates.list[i]
                if (sim >= 1.0) break
            }
        }
        if (bestName == null || bestScore < threshold) return null
        return bestName to bestScore
    }
}
