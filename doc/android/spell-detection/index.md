# spell-detection

풀로딩 화면 카드의 두 스펠 아이콘을 인식해 슬롯 spell1/spell2 default를 자동 설정.

기존 자동 흐름:
- 인게임 채팅 시스템 메시지 OCR → 슬롯 ready 트리거 (`OcrProcessor.triggerSlotSpell`)
- 단점: 메시지가 매번 잘 잡힌다는 보장 없음. spell1/2 default가 잘못이면 매칭 자체 실패.

목표:
- 풀로딩에서 적 5명 각자 두 스펠을 식별 → SlotState.spell1/2에 박음
- 오버레이 슬롯 텍스트(F/I/...) 대신 스펠 아이콘 이미지 표시

문서:
- [enemy-spell-from-loading.md](enemy-spell-from-loading.md): 카드 좌표 + crop + 매칭 알고리즘
- [spell-icons.md](spell-icons.md): 7종 reference 이미지 번들·캐싱
- [overlay-display.md](overlay-display.md): 슬롯 spell 버튼을 텍스트 → 이미지로
