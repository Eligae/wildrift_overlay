# 티어 cohort 탭 + 정렬 토글

`TierActivity`에 사이트 lolm.qq.com과 동일한 4개 cohort 탭과, 표 컬럼별 정렬을 추가한다.

## cohort 탭
실측 매핑(Nora SUP ban 80.69% 일치로 검증):

| 라벨 | 텐센트 mode |
|------|------------|
| 다이아+ | 0 |
| 석사+ | 1 |
| 왕 | 2 |
| 챌린저 | 4 |

mode 3은 정체 불명 → 미노출.

## 정렬
컬럼: 승률 / 픽률 / 밴률. 헤더 탭으로 토글(asc ↔ desc). 기본 = strength 오름차순(=사이트 기본).

## UI 결정
- cohort 탭: 라인 탭 **위 별도 행**.
- 정렬: 컬럼 헤더 클릭 → ↑↓ 토글. `TABLE` 모드만. `RECOMMEND` Top5는 strength 고정(=추천 의미 유지).

## 영향 범위
- 서버: `buildTierTable`이 4개 mode 모두 빌드, 캐시 `Record<cohort, TierTable>`. API `/v1/tier?cohort=` 쿼리.
- 클라이언트: cohort 탭(라인 탭과 별도 row), `TierAdapter` 정렬 prop, 헤더 ↑↓ 아이콘.
- 캐시: cohort 키도 함께 저장.

세부는 [server.md](server.md), [android-client.md](android-client.md) 보강.
