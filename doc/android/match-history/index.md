# match-history

게임 한 판이 끝났을 때 OCR로 감지하고, 그 판의 적팀/우리팀 챔피언 매핑을 영구 저장 + 사용자 검증 + 전적 화면에서 조회.

- [end-detection.md](end-detection.md): 게임 종료 OCR 시그널 + state 머신
- [storage.md](storage.md): MatchRecord 모델 + 저장소 (JSON SharedPreferences PoC)
- [verify-flow.md](verify-flow.md): 종료 직후 사용자에게 매핑 확인·수정 다이얼로그
- [review-ui.md](review-ui.md): 전적 리스트 + 상세 화면 (전적 사이트 톤)
