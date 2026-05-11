# 로드맵 — PR 단위

각 PR은 독립 빌드 + 실기 검증 가능 단위.

## MVP
1. Gradle 셋업, 빈 `MainActivity`.
2. `SYSTEM_ALERT_WINDOW` 부여 안내 + 인텐트.
3. `OverlayService` (포그라운드/알림) + 빈 오버레이.
4. 슬롯 5 × 버튼 3 레이아웃, 동작은 더미.
5. `SlotState` + 타이머 + 시각 상태 4단계.
6. 손잡이 드래그/더블탭 + SharedPreferences 직렬화.

## 첫 출시 (v0.1.0)
7. GitHub Actions 빌드/서명 + 첫 Release ([distribution](distribution.md)).
8. GitHub Pages 소개 ([../website.md](../website.md)).

## v1.x 이후
- 설정 토글 / 자동 채팅 감지
- 챔피언 추천 ([`../recommendation/`](../recommendation/))
- 조합 시너지/카운터 ([`../composition/`](../composition/))
- iOS 트랙
