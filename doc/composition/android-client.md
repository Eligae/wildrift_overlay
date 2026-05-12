# Android 측 — CompositionActivity

`MainActivity` "조합 보기" 버튼 → `CompositionActivity`. 단일 화면 두 모드 토글.

## 두 모드
- **시너지**: 팀원 4명 선택 → 부족한 역할 채우는 5번째.
- **카운터**: 적 1명 선택 → 카운터 후보 3~5명 (서버 큐레이션 한정).

## 챔피언 선택
**검색 가능 다이얼로그**:
- `EditText` 상단 + `ListView` 하단.
- 한국명 부분 일치 필터 (`contains`).
- 챔피언 목록은 `/v1/champions` 응답 (앱 캐시 미적용 — v2).

## 결과 카드
한 행에 [원형 아바타 48dp · 챔피언명 · 사유/메모]. `bg_tier_row` 재사용. 아바타는 Coil + 텐센트 CDN.

## 진입
`MainActivity` 두 번째 버튼.
