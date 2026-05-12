# 백엔드 서버 (Railway)

Node.js + TypeScript Express 서비스. 배포는 [deploy](deploy.md).

## 구성
- 언어: Node.js 20 + TypeScript (NodeNext 모듈).
- 호스팅: Railway Hobby plan (월 $5 수준).
- 저장: DB 없음. 메모리 캐시 + 번들 JSON 파일.
- cron 주기: 매일 03:00 UTC.

## cron 작업
1. [tencent-api](tencent-api.md) 두 endpoint fetch → 정규화.
2. [champion-mapping](champion-mapping.md)과 조인 → 한국어명 부착.
3. 결과를 메모리 캐시에 저장.

## API
- `GET /v1/lanes` → 라인 목록 `["TOP","JUG","MID","ADC","SUP"]`.
- `GET /v1/tier?lane=MID` → 단일 라인 `[{heroId, krName, winRate, ...}]`.
- `GET /v1/tier` → 전체 라인 `{TOP: [...], JUG: [...], ...}` (티어표용).
- `GET /v1/champions` → 한국어 매핑된 챔피언 목록 (조합 화면 picker용).
- `GET /v1/composition/synergy?team=10001,10002,...` → 부족한 역할 채우는 5번째 추천.
- `GET /v1/composition/counter?enemy=10037` → 카운터 매트릭스 조회.
- `POST /v1/refresh` → cron 수동 트리거.

## 오프라인 폴백
앱이 마지막 응답을 SharedPreferences 캐시 → 서버 장애 시 캐시 + "오래된 데이터" 배너. (현재 미구현, v1.1)
