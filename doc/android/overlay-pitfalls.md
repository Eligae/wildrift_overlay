# 오버레이 구현 핵심 함정

미리 박아두지 않으면 한 번씩 데이는 항목들.

- **터치 인터셉트**: `flags |= FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE`. 없으면 게임 입력을 통째로 빨아들임.
- **권한 부여**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` 인텐트. 첫 실행 시 안내 → 시스템 설정 → 복귀 재확인 루프.
- **포그라운드 서비스 타입**: Android 14+ 필수. `specialUse` 사용 시 `<property foregroundServiceTypeUseCase>` 함께 선언. 누락 시 런타임 크래시.
- **알림 채널**: 별도 채널, `IMPORTANCE_LOW`.
- **틱 주기**: 1초 단위 표시는 `Handler.postDelayed`. 임박 상태 깜빡임만 `Choreographer`로 전환.
- **상태 저장**: SharedPreferences로 짧은 주기 직렬화. DB는 v2.
