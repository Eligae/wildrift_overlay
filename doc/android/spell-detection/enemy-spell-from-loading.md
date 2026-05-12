# 풀로딩 → 적 스펠 추출

## 기본 가정
풀로딩 카드 5장 × 두 스펠 아이콘 = 10개 작은 ROI.
OCR로는 텍스트가 없어 불가 → 이미지 매칭 필요.

## 좌표계
ML Kit + SpellMatcher 모두 **rotate90 후 rotated frame** 좌표계로 통일
(`OcrProcessor.prepare`가 항상 rotate90 적용, rotationDegrees=0).
→ Pick.centerX/Y 그대로 crop 좌표로 사용 가능.

## 카드 안 스펠 offset 비율 (rotated frame H 기준, 1차 추정)
- `SPELL_BOX_H_RATIO = 0.045` — 스펠 박스 크기
- `SPELL_DY_RATIO = 0.035` — 라벨 아래쪽
- `SPELL_DX_RATIO = 0.025` — 라벨 중심에서 좌·우

라벨 = `LoadingScreenParser.Pick.centerX/Y` (챔피언 이름 OCR 박스 중심).
스펠 2개 위치: `(cx ± dx, cy + dy)` 정사각형.

**첫 실기 캡처로 비율 보정 필요**. 매칭 실패가 잦으면 `SpellMatcher.match(threshold)` 도 같이 조정.

## 매칭 알고리즘
1. 카드 좌표로 스펠 영역 crop (위 비율)
2. 7종 reference dHash 8x8 (Long 64bit) — `SpellMatcher.referenceHashes`
3. Hamming distance 최소 후보 선택, threshold(14) 초과면 null
4. null이면 SlotState.spell1/2 기존값 유지

## 흐름 통합 (구현 완료)
`OcrProcessor.broadcastEnemiesIfPass` 안에서:
- 슬롯 챔피언 갱신 → `detectEnemySpells(scaled, teams, prefs)` 호출
- `teams.enemies[i]`로 enemy slot 챔피언 확정 → `teams.picks` 에서 Pick 좌표 lookup
- 두 스펠 crop → `SpellMatcher.match` → `SlotState.spell1/2` 갱신 + saveSlot
- 그 후 reloadAll broadcast (기존 흐름)

## fallback
매칭 실패 시 spell1=FLASH, spell2=IGNITE (또는 기존값) 유지.
사용자가 long-press로 수정 가능 (기존 동작).
