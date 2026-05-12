# 라인 순서 — 좌→우 = TOP/JUG/MID/ADC/SUP

## 사용자 확정
10명 로딩 화면에서 한 row 5명의 얼굴 순서:
**왼쪽 → 오른쪽 = 탑 / 정글 / 미드 / 원딜 / 서폿**.

## OCR 좌표 → 게임 가로 화면 좌→우 매핑
폰 portrait 캡처, `InputImage.fromBitmap(bitmap, 90)`로 ML Kit에 90° 회전 지시.
좌표는 회전 후 frame 기준.

회전 후 frame 안에서 한 row 5명이 **세로**로 배치 (게임 가로 카드가 회전으로 누워있음).
즉 y가 작은 카드 ↔ 게임 화면 좌측(TOP) — **검증 필요**.

## 검증 절차
다음 게임에서:
- 적팀 슬롯 1~5에 박힌 챔피언과 실제 게임 첫 카드(TOP) 비교.
- 순서가 뒤집혀 있으면 sortedBy를 reverse.

## 임시 코드 가정
`sortedBy { it.second(=centerX) }` → 현재. centerX 작은 게 TOP.
검증 후 필요시 reverse 또는 sortedBy { -it.y } 같은 축 변경.
