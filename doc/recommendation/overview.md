# 챔피언 추천 트랙

라인별 추천 카드 + 전체 라인 티어표를 lolm.qq.com 데이터에서 한국어로.

## 흐름
1. 사용자가 라인 선택(추천 카드) 또는 전체 보기(티어표).
2. 앱이 자체 백엔드 API에 요청.
3. 백엔드가 정규화된 데이터 응답.
4. Android 화면에 추천 카드 또는 티어표.

## 트랙 내부 문서
- [백엔드 서버](server.md)
- [데이터 출처](data-source.md)
- [텐센트 API endpoint](tencent-api.md)
- [챔피언 / 라인 매핑](champion-mapping.md)
- [Android 클라이언트](android-client.md)
- [티어표 화면](tier-table.md)
- [Railway 배포](deploy.md)
- [결정 보류](pending.md)

## 우선순위
**v1.x 기능**. 인게임 트래커 안정화 + 첫 출시 후 착수.
