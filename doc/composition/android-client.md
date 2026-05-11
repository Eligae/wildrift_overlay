# Android 측 — 조합 추천 화면

[추천 트랙 클라이언트](../recommendation/android-client.md)와 별개 모드. 같은 메타 화면 안에 탭 또는 별도 진입.

## 두 모드
- **시너지**: 우리 팀 4명 선택 → [synergy-rules](synergy-rules.md) → 추천 5명.
- **카운터**: 적 1명 선택 → [counter-matrix](counter-matrix.md) → 추천 3~5명.

## 진입
`MainActivity` "조합 보기" 또는 추천 화면 상단 탭 확장.

## 입력 UX
- 챔피언 검색창 (한글명 fuzzy 검색).
- 또는 카드 그리드 (자주 쓰는 챔피언 즐겨찾기).

## 데이터
태그 / 매트릭스 JSON은 앱에 번들. 패치 시 앱 업데이트로 갱신 (서버 fetch 안 함).
