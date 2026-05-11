# 자동 채팅 감지 (주력)

WR이 적 스펠 사용 시 채팅에 **정형 시스템 메시지**를 뿌린다. 이걸 캡처해 슬롯 카운트다운을 자동 시작.

## 우선순위
실기 검증: 수동 탭이 게임 가림 + 정밀도 부족 → **자동 입력이 주력**. 수동 탭은 폴백.

## 진행 상태
PoC: `AccessibilityChatService` 작성, WR 패키지(`com.riotgames.league.wildrift`)로 좁힘. 실기 검증 대기 — 노드 트리에 채팅이 잡히면 옵션 A, 아니면 옵션 B로 전환.

## 매칭
- 정형 → 정규식 한 줄. 한국어 클라이언트 우선.
- 챔피언명 + 스펠명 추출, 5초 디바운싱.
- 선행: 메시지 샘플 수집 후 패턴 확정.

## 옵션 ([pending](pending.md))
- A: AccessibilityService
- B: MediaProjection + OCR (ToS 갱신 필요)
