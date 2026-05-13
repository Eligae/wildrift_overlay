# Fingerprint + 매칭 알고리즘

오디오 클립을 시간×주파수 행렬로 변환 → 정규화 → 템플릿과 코사인 유사도 비교.

## 처리 파이프라인

```
PCM 16bit mono
  ↓ frame 분할 (25ms 윈도우, 10ms hop, Hann)
  ↓ FFT 512 (실수→복소수)
  ↓ Power spectrum
  ↓ Mel filter bank (40 bands, 80~8000Hz)
  ↓ log (ε=1e-6)
  ↓ frame별 L2 정규화
Mel feature matrix [frames × 40]
```

## 라이브러리 부담
- FFT: 자체 구현 (Cooley-Tukey radix-2, 50줄). 라이브러리 X.
- Mel filterbank: 40개 삼각 필터 사전 계산해서 dot product.
- 모두 Float 연산, JIT가 알아서 SIMD 변환.

## 매칭

템플릿 길이 T, 입력 윈도우 W (W ≥ T). 입력에서 sliding offset s ∈ [0, W-T]:

```
score(s) = Σ_i ( template[i] · input[s+i] )    // 코사인 정규화 후 합산
```

최대 score 위치의 점수가 threshold (≈0.85) 초과 시 hit.

## 짧은 트리거 보호
- 길이 ≥ 1.0s 권장.
- 매치 후 cooldown(예: 5초) — 같은 hit 중복 발화 방지.

## 저장 형식
`.npfp` 바이너리:
- magic 4B "WRFP"
- frames: u16
- bands: u16 (= 40 고정)
- floats: frames × 40 × 4B

라벨, 생성시각, 길이 등은 메타 JSON 별도.
