# WildRift Spell Check Overlay

와일드리프트 인게임 적 스펠 쿨다운을 추적하는 Android 오버레이 + 한국어 티어/조합 추천 백엔드.

## tree

- `app/` — Android (Kotlin, AGP 8.7, minSdk 26)
- `server/` — Node.js Backend (Express + cron)
- `doc/` — 기획 문서 ([idea.md](doc/idea.md) 마스터)
- `docs/` — GitHub Pages

## start

- App

```bash
./gradlew installDebug
```

- Server

```bash
cd server && npm install && npm run dev
```
