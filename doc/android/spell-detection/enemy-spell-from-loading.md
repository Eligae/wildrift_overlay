# 풀로딩 → 적 스펠 추출

## 기본 가정
풀로딩 카드 5장 × 두 스펠 아이콘 = 10개 작은 ROI.
OCR로는 텍스트가 없어 불가 → 이미지 매칭 필요.

## 좌표 발견
캡처 PNG로 카드 layout 확인. 1080×2340 portrait 폰 기준:
- 적팀 column x ≈ 1010
- 카드 5개 y center: 254, 397, 540, 681, 824 (회전 frame 기준)
- 카드 안의 스펠 아이콘 두 개: 챔피언 이름 라벨 아래쪽, 카드 좌하단·우하단

정확한 offset은 첫 캡처로 확인 후 상수화. 폰 기종 차이는 비율로 저장.

## 매칭 알고리즘 (PoC)
1. 카드 좌표로 스펠 영역 crop (예: 24×24)
2. 7종 reference 이미지(FLASH/IGNITE/HEAL/GHOST/EXHAUST/BARRIER/SMITE)와 평균 색상 또는 perceptual hash(dHash 8bit) 비교
3. distance 가장 작은 후보 선택. threshold 초과면 기본값 유지

OpenCV는 무거움 — 자체 dHash 구현 정도가 PoC.

## 흐름 통합
`OcrProcessor.handleResult`의 풀로딩 분기에서:
- enemies 슬롯 매핑 후 각 슬롯 챔피언이 있으면 그 카드 좌표에서 두 스펠 crop → 매칭
- SlotState.spell1/spell2 갱신 + saveSlot
- 결과 broadcast → reloadAll

## fallback
매칭 실패 시 spell1=FLASH, spell2=IGNITE 기본값 유지. 사용자가 long-press로 수정 가능 (기존 동작).
