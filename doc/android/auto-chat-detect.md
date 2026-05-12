# 자동 채팅 감지 (PoC 검증됨)

WR이 적 스펠 사용 시 채팅에 시스템 메시지를 뿌린다. 이걸 캡처해 슬롯 카운트다운을 자동 시작.

## 검증 결과 (2026-05-11~12)
- 옵션 A (AccessibilityService): **불가**. WR 채팅이 SurfaceView/OpenGL이라 노드 트리에 텍스트 없음.
- 옵션 B (MediaProjection + ML Kit Korean OCR): **가능**. 한국어 인식 작동 (`미니언들이 생성되었습니다!` 정확). 다만 인게임 시스템 메시지 폰트가 작아 일부 깨짐 (`정화` ↔ `점화` 등).

## 정확도 보강 (사용자 제안)
**로딩 화면 사전 분석** — 적 5명 챔피언 + 스펠 2개씩 미리 등록 → 인게임 OCR에서 챔피언명만 정확히 잡으면 스펠 후보 2개로 좁혀짐 → fuzzy 매칭 정확도 ↑. iOS 트랙(`../ios/analyzer-notes.html`)과 같은 패턴.

## 현재 코드
- `ScreenCaptureService`: MediaProjection으로 한 프레임 캡처 + ML Kit OCR + ChatParser.
- `ChatParser`: 챔피언명 화이트리스트 + 스펠 alias(점화/정화 보정 포함). 매치 시 logcat `MATCH: ...`.

## 다음 단계
1. **로딩 화면 분석** 기능 — 챔피언 5명 + 스펠 사전 등록.
2. 채팅 영역 ROI 크롭 + 2x upscale로 작은 글자 인식률 향상.
3. 주기적 캡처 (PoC는 단발) — 발열·배터리 검토.
4. 매치 결과 → OverlayService로 broadcast → 슬롯 자동 트리거.
