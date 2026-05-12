# 게임 종료 감지

## 시그널
와일드리프트 종료 화면에 한국어 텍스트로 나오는 문구를 OCR 감지.

- 승리: "승리", "VICTORY", "WIN" 한 자라도 매칭
- 패배: "패배", "DEFEAT", "LOSE"

`OcrProcessor`가 매 OCR 결과의 `result.text`에 substring 검색.

## state 머신
세 상태:
1. **idle** — 캡처 중 아님 또는 픽 화면 전
2. **in_match** — 풀로딩 broadcast 이후 + 종료 시그널 전. 슬롯에 챔피언 박힘.
3. **end_detected** — 승/패 OCR 감지. verify-flow 트리거 + state idle 복귀 + slot reset.

`OverlayPrefs.matchState` (Int) 또는 enum 박아 추적.

## 중복 방지
종료 화면이 여러 캡처에 걸쳐 매칭될 수 있음 (3~10초). state == end_detected면 재발화 안 함.
다음 풀로딩 broadcast 또는 캡처 stop/start 시 idle로 복귀.

## 저장 트리거
종료 감지 즉시 현재 슬롯 데이터 + anchor를 `MatchRecord`로 직렬화하고 [storage.md](storage.md)에 push. verify-flow 결과로 수정 가능.
