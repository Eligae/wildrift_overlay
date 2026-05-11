# v0.1.0 — 부가 명세

## 시스템
- minSdk 26 / targetSdk 35.
- 포그라운드 서비스 `foregroundServiceType="specialUse"` + `<property>` 선언.
- 알림 채널 `overlay_service`, `IMPORTANCE_LOW`.
- 오버레이 flag: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`.

## 저장
SharedPreferences `overlay_state`. 키 prefix `slot_<index>_*`, `overlay_x/y`, `collapsed`.

## 한계
- 시작 버튼 비동기 → 300ms postDelay UI 갱신 hack.
- `POST_NOTIFICATIONS` 런타임 권한 미구현 (Android 13+ 알림 안 뜸, 서비스는 정상).
- 자동 release / Pages 미구현.
- 챔피언 이름·라인 표시 없음 (P1~P5만).
- 색상·폰트 튜닝 미진행.

## 코드 위치
- Manifest: `app/src/main/AndroidManifest.xml`
- Service / View / 상태 / Activity: `app/src/main/java/com/eligae/wrspellcheck/`
