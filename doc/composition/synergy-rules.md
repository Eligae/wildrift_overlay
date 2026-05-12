# 시너지 규칙 엔진

우리 팀 4명의 [태그](champion-tags.md)를 보고 부족한 역할 추론 → 그 역할에 맞는 챔피언 후보 5명 출력.

## 규칙 (현재 구현)
1. **탱커 부재** — class에 `tank`/`fighter` 없으면 탱커 후보 가산.
2. **하드 CC 부재** — `cc=hard` 없으면 하드 CC 후보.
3. **원거리 부재** — `range=ranged` 캐리 없으면 마크스맨/메이지 후보.
4. **이니시 부재** — `roles`에 `engage` 없으면 이니시 후보.
5. **AD 편중** — AD 4명 이상 → AP 후보.
6. **AP 편중** — AP 4명 이상 → AD 후보.

## 출력
규칙별 만족 사유 누적 → 사유 많은 순 5명. 각 추천에 한국어 사유 (`"팀에 탱커가 없습니다"` 등).

## 코드
`server/src/composition.ts::suggestSynergy`. 챔피언 태그 자산은 `server/data/champion-tags.json` (~100명).

## v2
패치별 메타 가중치 동적 조정. 라인 분석 (탑/정글/...) 반영.
