# 전적 리스트·상세 UI

## 진입
MainActivity에 "전적 보기" 버튼 추가 (티어/조합과 같은 라인).

## 리스트 (`MatchHistoryActivity`)
RecyclerView 한 행:
- 좌측: 결과 칩 (W gold / L hex teal 흐릿)
- 가운데: 적팀 5명 초상화 가로 배열
- 우측: 종료 시각 (오늘 12:34 / 어제 / N일 전)

상단 카운트 — `오늘 N판 (W승 L패)`.

## 상세 (`MatchDetailActivity` 또는 BottomSheet)
- 결과 + 시간
- 두 팀 한국명 + 초상화 + 라인 라벨 (TOP/JUG/MID/ADC/SUP)
- 액션: 수정 (→ verify-flow 재진입), 삭제

## 빈 상태
"기록된 전적 없음. 캡처가 켜진 상태로 한 판 진행하면 자동 저장돼요." 안내.

## 톤
LoL serif italic 제목 + gold/hex accent — 기존 화면 톤 일관.
