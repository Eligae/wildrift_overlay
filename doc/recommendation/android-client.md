# Android 측 — 챔피언 메타 화면 (TierActivity)

인게임 오버레이와 별개. **단일 화면에 두 모드 토글** (추천 / 전체).

## 두 모드
- **추천 카드** (기본 진입): 라인 상위 5명 큰 카드 (64dp 아바타 + 큰 이름 + 큰 승률). 1~3위는 hex teal 2dp 테두리 강조. [tier-table](tier-table.md)에서 갈라져 나옴.
- **전체 표**: 행 한 줄 형식. 모든 챔피언 (라인별 30~40명).

## 진입
`MainActivity` 하단 "티어 보기" 버튼 → `TierActivity`.

## 화면 구성
1. **상단 모드 toggle** (추천 | 전체) — 직접 텍스트 두 개, 활성 = gold.
2. **라인 탭** (TOP/JUG/MID/ADC/SUP) — Material TabLayout.
3. **상태 텍스트** — "갱신: 2026-05-12 19:41".
4. **RecyclerView** — 모드에 따라 RecommendAdapter / TierAdapter.

## 데이터
[server](server.md) `/v1/tier` 전체 응답 한 번 fetch. 두 모드 동일 데이터 공유.

## 오프라인 / 첫 실행
서버 미응답이면 "오류: ..." 메시지. 로컬 캐시 미구현 (v2).
