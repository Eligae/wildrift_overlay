# 챔피언 / 라인 매핑 (큐레이션)

[tencent-api](tencent-api.md)는 `hero_id`(숫자) + `position`(1~5)만 준다. 사용자 표시용 한국어명·라인명은 우리가 매핑.

## `heroId → 한국어명`
- 큐레이션 JSON. 예: `{"10001": "가렌", "10002": "아트록스", ...}`
- 신규 챔피언 출시 시 PR로 추가.
- 1차 자산은 저장소 안 (`server/data/champion-kr.json` 같은 경로 예정).

## `position → 라인` (추정, 실측 필요)
- 1 = top(상단)
- 2 = jungle(정글)
- 3 = mid(중원)
- 4 = bot(하단)
- 5 = support(서포터)

응답을 라인별 챔피언 면면과 대조해 확정. [pending](pending.md).

## 갱신
패치마다 신규 챔피언 1~2명 추가 가능 → 매핑 누락 시 백엔드가 한국어명 대신 alias(병음) 폴백.
