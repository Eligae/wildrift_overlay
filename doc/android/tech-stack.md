# 기술 스택

- 언어: **Kotlin**
- UI: 프로그래매틱 View (Compose 없음 — 오버레이에서 LifecycleOwner / SavedStateRegistry 셋업 부담 회피)
- 오버레이: `WindowManager` + `TYPE_APPLICATION_OVERLAY`
- 백그라운드: `Service` + `foregroundServiceType="specialUse"` (Android 14+)
- 최소 SDK: API 26 (Android 8.0)
- 타깃 SDK: 35
- 외부 의존성: AndroidX core-ktx, appcompat, Material — 그 외 없음

크로스플랫폼 프레임워크 기각 근거는 [why-native](why-native.md) 참조.
