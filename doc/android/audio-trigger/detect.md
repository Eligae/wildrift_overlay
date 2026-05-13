# 실시간 검출 + 트리거 발화

캡처 서비스가 가동 중일 때 오디오 ring buffer에서 1초마다 fingerprint 추출 → 등록된 템플릿들과 비교.

## 검출 루프

```kotlin
while (capturing) {
    val mel = computeMelFeatures(ringBuffer.snapshot(8s))
    for (tpl in loadedTemplates) {
        val score = bestSlidingScore(mel, tpl.features)
        if (score >= tpl.threshold && cooldownExpired(tpl)) {
            fireTrigger(tpl)
            cooldown[tpl] = now + 5000ms
        }
    }
    sleep(1000ms)
}
```

## 트리거 발화 (action별)
- `match_start` → `OverlayPrefs.matchStartedAtMs = now; matchEndDetected=false`
- `match_win/lose` → 직접 MatchHistoryStore.add() 호출 (현재 enemies/allies prefs 기반) + verify notify
- `ult_ready` → 등록 시 사용자가 매핑한 slot의 `setSlotSpellReady()` 호출
- `custom` → `Log.d(TAG, "TRIGGER: $label")` 만

## 성능
- 8s × 100 frames/s × 40 bands = 32000 floats 행렬. 매 1초마다 재계산.
- FFT 800회/초 (8s × 100 hop) → 수십 ms 수준.
- 매칭: 템플릿 5개 × 길이 200 frames × 40 bands × 슬라이딩 ~600 offsets = 24M float multiply/sec — 모바일 CPU에서 < 5%.

## 신뢰도 보강
- 동일 트리거가 N초 내 재발화 안 되게 cooldown.
- 매치 진행 상태(`matchStartedAtMs>0`)에 따라 어떤 트리거를 액티브로 둘지 게이팅 (예: match_start는 매치 안 켜져 있을 때만).

## 디버그
- 검출 점수 logcat 노출 (Log.d).
- "최근 점수 그래프" 디버그 화면 (선택) — threshold 튜닝용.
