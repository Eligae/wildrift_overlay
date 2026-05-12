# Activity 안 fetch/adapter 분리

## TierActivity (117줄)
RecyclerView + 데이터 fetch + UI 상태. 이미 TierAdapter 분리됨. 추가 분리는 ViewModel — 비용 ↑. 보류.

## CompositionActivity (286줄)
가장 큰 Activity. 책임:
1. RecyclerView 두 종류 (synergy / counter)
2. 챔피언 선택 UI (5명 4명)
3. ChampionsCache · API 호출 · suggestSynergy / suggestCounter 호출
4. 결과 표시

## 분할 안
- `CompositionActivity` — onCreate + view 바인딩만 (~80줄)
- `CompositionPresenter` — 데이터 fetch + 추천 로직 + selectedChampions state (POJO 또는 class)
- `ChampionPickerSheet` — 챔피언 선택 BottomSheet/Dialog
- `RecommendAdapter` (기존) — list 어댑터

## MainActivity (226줄)
- 권한 + 슬라이더 2개 + 캡처 트리거 + 업데이트 배너. 작은 책임이 많음.
- helper 함수 (progressToScale 등) → `OverlayLayoutUiHelpers.kt`
- 업데이트 배너 → `UpdateBannerController.kt` (배너 view + onClick + dismiss)

분할 후 MainActivity 자체는 ~120줄.
