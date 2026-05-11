# 백엔드 서버 (Railway)

Node.js 또는 Python 작은 서비스. Railway 배포 + cron 갱신 + REST API.

## 구성
- 언어: Node.js / Python(FastAPI) 후보.
- 호스팅: Railway Hobby plan (월 $5 수준).
- 저장: DB 없음. 메모리 캐시 + JSON 파일.
- cron 주기: 하루 1회.

## cron 작업
1. [tencent-api](tencent-api.md) 두 endpoint fetch → 정규화.
2. [champion-mapping](champion-mapping.md)과 조인 → 한국어명 부착.
3. 결과를 메모리/디스크에 저장.

## API
- `GET /v1/tier?lane=top` → 단일 라인 `[{heroId, krName, winRate, pickRate, banRate}, ...]`.
- `GET /v1/tier` → 전체 라인 `{top: [...], jungle: [...], mid: [...], bot: [...], support: [...]}` (티어표용).
- `GET /v1/lanes` → 라인 목록.

## 오프라인 폴백
앱이 마지막 응답을 SharedPreferences 캐시 → 서버 장애 시 캐시 + "오래된 데이터" 배너.
