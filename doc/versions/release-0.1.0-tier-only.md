# v0.1.0 릴리스 — 티어 단독 모드

OCR/오버레이 흐름이 아직 불안정해 release에선 **티어 화면만 노출**한다.

## 동작 사양
- `btn_tier` → 정상 진입(유일하게 사용 가능한 기능).
- 다음 버튼은 클릭 시 **Toast "개발중입니다"** 표시, 실제 진입 없음:
  - `btn_grant`, `btn_start`, `btn_capture`
  - `btn_composition`, `btn_calibration`, `btn_match_history`
- 슬라이더(scale/alpha)·pass-through 스위치는 보이지 않게 숨김(오버레이 비활성 상태에선 무의미).
- 업데이트/뉴스 배너는 유지.

## 웹페이지
`docs/index.html`의 OCR/오버레이 항목은 "(개발중)" 뱃지로 명시.
티어/조합 표는 정상 기능으로 강조.

## 다음
OCR 안정화되면 단계적으로 버튼 재활성화.
