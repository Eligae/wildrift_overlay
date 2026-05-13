# 자기 챔피언 식별 (hybrid: 색상 OCR → 수동 fallback)

## 1단계: 노란색 닉네임 자동 감지 (primary)
와일드리프트 로딩/픽 화면에서 **본인 닉네임만 노란색(gold)** 으로 표시. OCR 텍스트 매칭 없이 픽셀 색상으로 식별 가능.

알고리즘:
1. ML Kit OCR로 모든 텍스트 블록 + bounding box 추출 (이미 진행 중).
2. 각 블록의 bbox 내 픽셀을 샘플링.
3. HSV 변환 후 hue ≈ 40~55°·sat > 0.3·val > 0.5인 픽셀(노란/금색) 비율 계산.
4. 노란 픽셀 비율 ≥ 2%이면 **사용자 닉네임 블록**.
5. 그 블록 중심점과 가장 가까운(centerY/X 거리 최소) `Pick`을 `userChampion`으로 확정 → `userSlot` 산출.

## 2단계: 수동 fallback
자동 감지 실패 시 (`userSlot == null`):
- VerifyMatchActivity 상단에 "내 챔피언 선택" 프롬프트.
- allies 5명 중 자기 픽 탭 → `userSlot` 저장.

## 비고
- 닉네임 입력 UI 불필요 — 색상 신호가 항상 존재.
- 색상 범위는 실기에서 ROI 캡처로 보정. WR UI 업데이트 시 hue 범위 조정 필요할 수 있음.
- 자동 100% 신뢰는 아님 — 황색 톤이 적팀에서 우연 검출되거나 OCR이 닉네임 bbox를 못 잡으면 실패.
