# 하위 패키지 분리 안

`com.eligae.wildrift.overlay.*` 아래로 의미별 하위 패키지를 도입.

```
overlay/
├── ui/         MainActivity, TierActivity, CompositionActivity, CalibrationActivity, TierAdapter, RecommendAdapter
├── floating/   OverlayService, OverlayView, SlotView, RoiOverlayView
├── capture/    ScreenCaptureService, CaptureSession, OcrProcessor, BitmapUtils
├── parse/      ChampionRegistry, ChatParser, LoadingScreenParser, SpellAliases
├── model/      Spell, SlotState
├── prefs/      OverlayPrefs, SlotPrefs, AllyAnchorPrefs, OcrRoiPrefs
├── update/     VersionCheck
├── cache/      ChampionsCache, TierCache
└── api/        ApiClient, Dto, GitHubRelease, WrApi (현재 유지)
```

## 이유
- 한 패키지에 17개 파일이 평면. 의미 응집 ↓.
- 패키지 import 보면 의존 방향이 그림. 순환·이상 의존 발견 쉬움.
- Activity가 Service / Parser / Prefs 다 import — 디렉토리만 봐도 알 수 있어야.

## 비용
- 패키지 변경 → 모든 파일의 `package` 선언 + import 다시. AndroidManifest의 service/activity android:name도 fully-qualified.
- IDE refactor (Move) 사용하면 자동.

## 점진 채택
한 패키지씩. parse/ → capture/ → floating/ → ui/ 순서가 안전 (의존 그래프 잎부터).
