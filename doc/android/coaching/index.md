# 전적 기반 코칭 (post-match coaching)

게임 끝나고 "내가 다른 챔피언을 골랐으면 어땠을까"를 알려주는 기능.

## 가치 제안
사용자가 매치 단위로 자기 픽을 회고 → 같은 적팀·우리팀(나 제외) 조합에서 권장된 픽과 비교 → 학습.

## 토대
- `MatchRecord` 이미 enemies[5] + allies[5] 보유. 하지만 "내 챔피언"이 어느 슬롯인지 모름 — 추가 필요.
- 서버에 `/v1/composition/synergy` (우리팀 N명 → 5번째 추천), `/v1/composition/counter` (적 1명 → 카운터 후보) 이미 있음. 재사용.

## 트랙
- [self-detect](self-detect.md) — 자기 챔피언 식별 (hybrid: 닉네임 OCR → fallback 수동 마킹).
- [suggest](suggest.md) — 시너지 + 카운터 두 축 추천 로직.
- [match-detail-ui](match-detail-ui.md) — 매치 상세에서 비교 표시 (작성 예정).

## 데이터 모델 변경
- `MatchRecord`에 `userSlot: Int?` (0~4, allies 인덱스) + `userName: String?` (OCR 추출 닉네임) 추가.
- 마이그레이션: 기존 30판 LRU는 그냥 두고, 신규 매치부터 채움. 옛 매치는 사용자가 Verify에서 수동 마킹 가능.
