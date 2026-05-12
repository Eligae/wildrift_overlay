# MatchRecord 저장

## 데이터 모델
```kotlin
data class MatchRecord(
    val id: Long,              // epoch ms — 종료 감지 시점
    val startedAtMs: Long,     // 첫 broadcast 시점 (in_match 진입)
    val endedAtMs: Long,
    val result: Result,        // WIN | LOSE | UNKNOWN
    val enemies: List<String>, // canonical 한국명 5명 (빈 자리 — null 슬롯 제외)
    val allies: List<String>,  // anchor 또는 사용자 수정 결과
    val userVerified: Boolean, // verify-flow 통과 여부
)

enum class Result { WIN, LOSE, UNKNOWN }
```

## 저장소
PoC 단순 — `SharedPreferences("match_history")` 안에 Moshi JSON List 한 줄.
30판 이상 쌓이면 가장 오래된 것부터 제거 (LRU). 영구 보관은 v2.

## API
```kotlin
class MatchHistoryStore(context: Context) {
    fun add(record: MatchRecord)
    fun update(id: Long, mutate: (MatchRecord) -> MatchRecord)
    fun loadAll(): List<MatchRecord>   // 최신순
    fun delete(id: Long)
    fun clear()
}
```

## 직렬화 키
기존 `ChampionsCache`와 같은 Moshi 인스턴스 재사용. 별도 SharedPreferences 파일.
