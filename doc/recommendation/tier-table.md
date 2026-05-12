# 티어표 모드

라인별 추천(상위 5명)과 함께 같은 `TierActivity`의 **두 번째 모드**.

## 가치 제안
"전체 메타 흐름" 한눈에. lolm.qq.com 중국 페이지의 표를 한국어로 옮긴 형태. 추천 카드보다 더 많은 챔피언 (라인당 30~40명).

## 화면
- 한 행: 순위 / 원형 아바타 44dp / 챔피언 한글명 / W·P·B% (monospace) / 큰 승률 gold + WIN 라벨.
- 1~3위는 hex teal 1dp 테두리 강조.

## 데이터
같은 [server](server.md) `/v1/tier` 응답 그대로. [android-client](android-client.md) 참조.

## 진입
[android-client](android-client.md)의 모드 toggle "전체".
