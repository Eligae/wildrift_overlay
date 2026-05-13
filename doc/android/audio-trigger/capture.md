# 오디오 캡처 인프라

`AudioPlaybackCaptureConfiguration` + `MediaProjection`로 WR 재생 오디오 캡처.

## API 셋업
```kotlin
val cfg = AudioPlaybackCaptureConfiguration.Builder(projection)
    .addMatchingUsage(AudioAttributes.USAGE_GAME)
    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
    .build()
val fmt = AudioFormat.Builder()
    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
    .setSampleRate(44100)
    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
    .build()
val rec = AudioRecord.Builder()
    .setAudioPlaybackCaptureConfig(cfg)
    .setAudioFormat(fmt)
    .setBufferSizeInBytes(bufSize * 4)
    .build()
```

## PCM 링 버퍼
- 길이: 8초 (44100*8*2 = ~700KB) — 가장 긴 트리거 사운드 + 여유.
- 별도 스레드(`HandlerThread`)에서 `rec.read(buf, n)` 연속 호출.
- 시점별 fingerprint 계산은 sliding window 1초마다 (overlap 7s).

## WR 캡처 정책 검증 (Phase 0 PoC)
- 5초 캡처 후 PCM 진폭 평균 확인.
- avg ≥ 100 (16-bit signed)이면 정상.
- avg ~0이면 WR이 `ALLOW_CAPTURE_BY_NONE` 박은 것 → fallback 필요(또는 기능 포기).

## 리소스 관리
- 캡처 서비스 중지 시 `rec.stop() + rec.release()` 호출.
- MediaProjection 토큰은 ScreenCapture와 공유 — 별도 권한 요청 X.
