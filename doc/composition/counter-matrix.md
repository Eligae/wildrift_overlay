# 카운터 매트릭스 (큐레이션)

"적 챔피언 X 카운터는 누구?" 표. 사람 큐레이션.

## 형식 (`server/data/counter-matrix.json`)
```json
{
  "10037": {
    "counters": ["10044", "10009", "10004"],
    "note": "이동기에 강한 CC. 풍벽 흘리기"
  }
}
```

- `counters`: heroId 3~5개, 중요도 순.
- `note`: 한 줄 한국어 메모.

## 현재 시드
~15명 (인기 챔피언 우선). 사용자가 점진적으로 보강 — 한국 와일드리프트 메타 기반.

## 출력
서버가 [champion-kr](../recommendation/champion-mapping.md)과 [hero list](../recommendation/tencent-api.md)와 조인해 한국명 + 아바타 URL 부착. `server/src/composition.ts::suggestCounter`.

## 한계
"제안" 수준. 룬/아이템/실력에 좌우. UI에 "절대답 아님" 명시.
