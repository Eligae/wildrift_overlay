# refactor

오버레이 앱 코드를 작은 파일·하위 패키지로 분리해 리팩토링 가능성을 높인다.

현재 평면 패키지 `com.eligae.wildrift.overlay` 하나에 17개 .kt + `api/` 만 있음.
가장 큰 파일: ScreenCaptureService 328 / CompositionActivity 286 / MainActivity 226.

- [package-layout.md](package-layout.md): 하위 패키지 분리 안 — capture/parse/prefs/ui/floating 등
- [screen-capture-split.md](screen-capture-split.md): ScreenCaptureService 책임 분할
- [prefs-split.md](prefs-split.md): OverlayPrefs를 도메인별 prefs로 분리
- [activities-split.md](activities-split.md): Activity 안의 fetch/adapter 분리
