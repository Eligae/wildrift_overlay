# Railway 배포

## 사전 준비
- Railway 계정 + CLI 설치 (`brew install railway` 또는 `npm i -g @railway/cli`)
- `railway login`

## 첫 배포
```bash
cd server
railway init                 # 새 프로젝트 생성, 로컬과 연결
railway up                   # 현재 디렉토리 빌드+배포 (Dockerfile 자동 인식)
```

도메인은 Railway 대시보드에서 `Generate Domain` → 자동 `<name>.up.railway.app` 할당.

## 환경 변수
- `PORT` — Railway가 자동 주입 (코드에서 `process.env.PORT ?? 3000`).
- 추가 변수 없음 (PoC).

## 앱 측 URL 교체
배포 후 받은 URL을 `app/build.gradle.kts`의 `buildConfigField("String", "API_BASE_URL", ...)` 에 박는다 (HTTPS면 `usesCleartextTraffic` 제거 가능).

## CI 자동 배포
GitHub 연동 후 `main` push 시 자동 빌드/배포 가능 (Railway 대시보드 Source = GitHub Repo).

## 한계
무료 한도 초과 시 Fly.io / Cloudflare Workers로 마이그레이션 (cron 호환성 검토).
