# ScreenCaptureService 분할

현재 328줄. 책임 7개 섞임.

## 책임 분해
1. Service lifecycle — start/stop, foreground 알림 → `ScreenCaptureService`
2. MediaProjection / VirtualDisplay / ImageReader 셋업·해제 → `CaptureSession`
3. ImageReader → Bitmap 변환 + 회전 → `BitmapUtils`
4. ML Kit OCR 호출 + 블록 추출 → `OcrProcessor`
5. ChatParser/LoadingScreenParser 매칭 → 기존 (parse/) 호출만
6. anchor 저장 + broadcast 발신 → `AnchorTracker` (또는 prefs/AllyAnchorPrefs 활용)
7. PNG 디스크 저장 (학습용) → `CaptureDebugSaver`

## 흐름 (분할 후)
```
Service → CaptureSession.next() → Bitmap → OcrProcessor.run() → OcrResult
        → ChatParser.parse / LoadingScreenParser.parseTeams
        → AnchorTracker.maybeUpdate / broadcastEnemies
        → CaptureDebugSaver.save (옵션)
```

## 보존
- INITIAL_DELAY_MS, INTERVAL_MS는 Service에. 캡처 주기 = service의 일.
- isRunning 플래그도 service.
- ROI crop은 OcrProcessor에 (prefs 조회 포함).

분할 후 Service 자체는 ~80~100줄로 감소 예상.
