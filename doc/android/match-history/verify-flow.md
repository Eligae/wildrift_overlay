# 종료 직후 사용자 검증

## 트리거
end-detection 발화 후 1~2초 안에 알림 또는 dialog.
오버레이 활성 상태에서 toast 또는 별도 Activity 띄우기 — `VerifyMatchActivity`.

## UI 흐름
1. 화면 상단: 결과(승/패) + 시간.
2. 적팀 5 슬롯 + 우리 팀 5 슬롯 — 각 슬롯에 챔피언 초상화 + 한국명.
3. 슬롯 탭: 챔피언 picker (전체 챔피언 그리드, ChampionsCache 사용). 선택 시 교체.
4. "맞아요" 버튼 → `userVerified=true` 저장 + 종료.
5. "비웠다(인식 안 됨)" 슬롯은 비워두고 사용자가 직접 채워야 verified=true 가능.

## OCR 누락 보강
인식 안 된 슬롯이 있으면 빨간 stroke로 강조 → 사용자 주의 유도.

## 자동 닫기
사용자가 아무 동작 안 하고 5분 지나면 `userVerified=false`로 자동 저장 + 종료.

## 데이터 라벨링 가치
`userVerified=true` 레코드를 v2에 OCR 정확도 평가 dataset으로 사용 가능.
