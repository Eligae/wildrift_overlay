# v0.1.0 — 부가 명세

## 시스템
- minSdk 26 / targetSdk 35.
- 패키지: `com.eligae.wildrift.overlay`.
- 포그라운드 서비스 `foregroundServiceType="specialUse"` + `<property>`.
- 알림 채널 `overlay_service`, `IMPORTANCE_LOW`. `POST_NOTIFICATIONS` 런타임 요청.
- 오버레이 flag: `FLAG_NOT_FOCUSABLE | FLAG_NOT_TOUCH_MODAL`.

## 저장
SharedPreferences `overlay_state`: `slot_<i>_spell{1,2}{,_ready}`, `overlay_x/y`, `collapse_level`, `scale`.

## 한계
- 시작 버튼 비동기 → 300ms postDelay hack.
- 자동 release / Pages 미구현.
- 챔피언 이름 없음 (라인 약자만 — TOP/JUG/MID/ADC/SUP).
- 궁극기 추적 제외 (v2 재검토).

## 실기 검증 결론
화면 가림 + 수동 탭 정밀도 부족 → v0.2.0에서 자동 채팅 감지 주력.

## 코드 위치
- Manifest: `app/src/main/AndroidManifest.xml`
- 코드: `app/src/main/java/com/eligae/wildrift/overlay/`
