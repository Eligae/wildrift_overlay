# 와일드리프트 스펠체크

와일드리프트 인게임 적 스펠 쿨다운을 추적하는 Android 오버레이 + 한국어 티어/조합 추천 백엔드.

## 트리
- `app/` — Android (Kotlin, AGP 8.7, minSdk 26)
- `server/` — Node.js 백엔드 (Express + cron)
- `doc/` — 기획 문서 ([idea.md](doc/idea.md) 마스터)
- `docs/` — GitHub Pages 소개

## 빠른 시작
- 앱: `./gradlew installDebug`
- 서버: `cd server && npm install && npm run dev`

빌드/배포는 [doc/android/build.md](doc/android/build.md), [doc/recommendation/deploy.md](doc/recommendation/deploy.md).

## 라이선스
MIT.
