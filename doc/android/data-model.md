# 데이터 모델 스케치

## `Spell`
스펠 종류 enum. 값과 기본 CD는 [`../spell-cooldowns.md`](../spell-cooldowns.md) 참조.

## `SlotState`
- `index: Int` (0..4)
- `spell1: Spell`, `spell2: Spell`
- `spell1ReadyAtEpochMs: Long?` — null이면 대기
- `spell2ReadyAtEpochMs: Long?`

## 컨테이너
오버레이는 `List<SlotState>` 한 덩어리. 현재 시각과의 차이로 남은 시간 계산.

## v2 확장
- 챔피언 이름/아이콘 라벨
- 슬롯별 어빌리티 헤이스트 보정
- 궁극기 추적 재도입 검토
