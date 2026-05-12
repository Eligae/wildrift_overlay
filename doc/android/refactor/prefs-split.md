# OverlayPrefs 분할

현재 96줄에 4개 도메인 섞임 — 슬롯 상태, 오버레이 위치/크기/알파, ROI 좌표, ally anchor.

## 분할 안
| 새 파일 | 책임 |
| --- | --- |
| `SlotPrefs` | slot_{i}_champion/spell{1,2}{,_ready} 5개 슬롯 read/write |
| `OverlayLayoutPrefs` | overlayX/Y, collapseLevel, scale, bgAlpha |
| `OcrRoiPrefs` | roiLeft/Top/Right/Bottom, hasCustomRoi |
| `AllyAnchorPrefs` | allyAnchor, allyAnchorAtMs, freshAllyAnchor() |

## 공유
모두 같은 SharedPreferences (`overlay_prefs`) — 헬퍼 함수 `fun overlayPrefs(ctx) = ctx.getSharedPreferences(...)` 하나 둠.

## 호출처 패치
- MainActivity: OverlayLayoutPrefs + SlotPrefs (현재 통합 prefs.scale/bgAlpha)
- OverlayService / OverlayView / SlotView: SlotPrefs + OverlayLayoutPrefs
- CalibrationActivity: OcrRoiPrefs
- ScreenCaptureService: OcrRoiPrefs + AllyAnchorPrefs + SlotPrefs

## 점진 채택
OverlayPrefs를 `Deprecated` typealias로 잠시 두고 4개로 동시 export. 호출처 한 줄씩 교체.
