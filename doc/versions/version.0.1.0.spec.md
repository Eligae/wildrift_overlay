# v0.1.0 — 부가 명세

## 시스템
- minSdk 26 / targetSdk 35.
- 포그라운드 서비스 `foregroundServiceType="specialUse"` + `<property>` 선언.
- 알림 채널 `overlay_service`, `IMPORTANCE_LOW`.
- 오버레이 flag: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`.

## 저장
SharedPreferences `overlay_state`. 키 `slot_<i>_spell{1,2}{,_ready}`, `overlay_x/y`, `collapsed`.

## 한계
- 시작 버튼 비동기 → 300ms postDelay hack.
- `POST_NOTIFICATIONS` 런타임 권한 미구현 (Android 13+ 알림 안 뜸).
- 자동 release / Pages 미구현.
- 챔피언 이름·라인 표시 없음.
- 궁극기 추적 제외 (v2 재검토).

## 실기 검증 결론
화면 가림 + 수동 탭 정밀도 부족 → v0.2.0에서 자동 채팅 감지 주력.

## 코드 위치
- Manifest: `app/src/main/AndroidManifest.xml`
- 코드: `app/src/main/java/com/eligae/wrspellcheck/`
