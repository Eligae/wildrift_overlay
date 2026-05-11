# 추천 트랙 결정 보류

- **추천 알고리즘** — 단순 승률 정렬 / 픽·밴율 가중치 / 카운터 매칭. v1.x 시작은 단순.
- **진입 UX** — 별도 Activity vs 오버레이 모드 전환. PoC 후 결정.
- **서버 언어** — Node.js vs Python (FastAPI).
- **`position` 1~5 ↔ 라인 매핑** — 응답 실측으로 확정. [champion-mapping](champion-mapping.md).
- **모드 키 `0`/`1` 의미** — 랭크/일반? 한쪽만 노출 vs 둘 다.
- **챔피언 한국어명 매핑 관리** — JSON in repo vs 별도 CMS.
- **앱 ↔ 서버 인증** — 익명 공개 vs API key. 익명 우선.
- **Railway 한도 초과 시 대안** — Fly.io / Cloudflare Workers.
