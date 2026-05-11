# 설정 화면 (MainActivity)

`MainActivity`는 권한 안내 + 오버레이 시작/중지 + 토글 설정을 모은 단일 화면. 별도 PreferenceActivity 분리하지 않는다.

## 구성
1. **권한 상태 배너** — `SYSTEM_ALERT_WINDOW` 미부여 시 빨간 배너 + 부여 버튼.
2. **오버레이 시작/중지** — 포그라운드 서비스 토글.
3. **옵션 토글들**
   - 자동 채팅 감지 (기본 OFF) — [auto-chat-detect](auto-chat-detect.md)
   - (향후 추가될 자리)

## 저장
SharedPreferences, 키 prefix `pref_*`. 슬롯 상태와는 별도 파일로 둔다.

## 별도 화면 두지 않은 이유
앱이 본질적으로 "오버레이만 떠 있는 도구". Activity 둘 이상은 사용자 동선만 늘어남.
