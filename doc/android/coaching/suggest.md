# 추천 로직 (시너지 + 카운터)

## 시너지 ("내 라인에 어울리는 다른 챔피언")
- 입력: allies 5명에서 `userChampion` 빼낸 4명.
- 호출: `GET /v1/composition/synergy?team=<heroId1>,<heroId2>,<heroId3>,<heroId4>` — 서버가 부족한 역할 채우는 5번째 추천.
- 표시: 권장 후보 N개 vs 내가 픽한 챔피언.

## 카운터 ("적 라인 상대로 더 나은 픽")
- 토대: 적 라인 매칭 필요. 슬롯 인덱스 = LANE_Y_RATIOS 순서대로 TOP/JUG/MID/ADC/SUP라서, **`userSlot`이 곧 내 라인**.
- 적 같은 라인 슬롯 = enemies[`userSlot`].
- 호출: `GET /v1/composition/counter?enemy=<enemyHeroId>` — 적 챔피언 카운터 후보.
- 표시: 카운터 후보 N개 vs 내가 픽한 챔피언.

## 보여줄 데이터
- "이 매치에서 [내 챔피언]을 픽함. 시너지 추천: [A], [B], [C]. 라인 카운터 추천: [X], [Y]."
- 사용자의 픽이 추천에 포함돼 있으면 "잘 픽했어요" 마크.

## 한계
- 슬롯 인덱스가 LANE_Y_RATIOS와 항상 일치한다는 가정 — 풀로딩 OCR이 정확해야 함.
- 카운터 매트릭스의 데이터 quality에 의존.
