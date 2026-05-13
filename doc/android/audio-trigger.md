# 인게임 사운드 트리거

특정 게임 사운드를 감지해 이벤트 핸들러(매치 시작, 승/패, 쿨다운 종료 등)를 정확한 시점에 발화. OCR 1.5s 폴링 보완.

## 핵심 아이디어

1. **사운드 등록 (학습)**: 사용자가 게임에서 트리거하고 싶은 소리(예: 매치 시작 팡파레)를 한 번 녹음 → 앱이 그 클립의 **fingerprint** 계산해서 저장.
2. **실시간 검출**: 캡처 중 들어오는 오디오 스트림에 sliding window로 같은 fingerprint를 계산해 등록된 템플릿과 비교 → 일치 시 trigger.

## 기술 스택

- **오디오 캡처**: Android 10+ `AudioPlaybackCaptureConfiguration` + `MediaProjection` 토큰 재사용. PCM 16-bit 44.1kHz mono로 받음. 별도 권한 X.
- **WR 캡처 정책**: `ALLOW_CAPTURE_BY_NONE`이면 무음 — **PoC로 사전 확인** 필수.
- **Fingerprint**: log-Mel spectrogram (frame 25ms hop 10ms, 40 mel bands, 8s 윈도우). 가벼움, OpenCV/ML 라이브러리 불필요.
- **매칭**: 코사인 유사도 (각 프레임 벡터 정규화 후 dot product 합산).

## 세부 트랙
- [audio-trigger/capture](audio-trigger/capture.md) — 캡처 인프라.
- [audio-trigger/fingerprint](audio-trigger/fingerprint.md) — Mel-spectrogram + 매칭 알고리즘.
- [audio-trigger/learn-ui](audio-trigger/learn-ui.md) — 사운드 녹음·라벨링 화면.
- [audio-trigger/detect](audio-trigger/detect.md) — 실시간 검출 + 트리거.

## 후보 사운드
- 매치 시작 팡파레 → `matchStartedAtMs` 정확한 박이는 시점.
- 승리/패배 효과음 → end 감지 OCR보다 ~1초 빠름.
- 궁극기/소환사 쿨다운 종료 사운드 → 슬롯 ready 자동.

## 한계
- WR 패치로 사운드 바뀌면 fingerprint 재등록 필요 (사용자 트리거).
- 짧은 사운드(≤500ms)는 false-positive 위험 — 최소 1초 이상 권장.
- 배경음/효과음 겹치면 신호 약해짐 — 매칭 threshold 보수적으로.
