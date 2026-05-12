# Contributing

기여 환영합니다. 아래 가이드를 따라주세요.

## PR 전 체크리스트

- [ ] 변경 의도가 issue 또는 PR 설명에 명확히 기술됨.
- [ ] **secret 또는 token이 커밋에 포함되지 않음** (`local.properties`, `.env`, `*.jks`, `*.keystore`는 .gitignore에 박혀 있음).
- [ ] 빌드/테스트 통과: `./gradlew installDebug`, `cd server && npm run build`.
- [ ] 새 아이디어는 코드 전에 `doc/` 에 기획 문서 먼저 (CLAUDE.md 규약).
- [ ] 한 문서 300자 이내 — 길면 하위 문서로 쪼개기.

## 코드 스타일

- Kotlin: 표준 IntelliJ 포매터. 패키지 구조는 `doc/android/refactor/package-layout.md` 참고.
- TypeScript: prettier 기본. 서버 라우트는 `server/src/routes.ts`에 집중.

## 커밋 메시지

subject line 한 줄만. body 없음. trailer는 별도. 예시: `fix: 챔피언 OCR alias 보정`.

## 챔피언/스펠 데이터

`server/data/champion-kr.json`의 한국명은 사용자 결정에 위임 — 임의 "공식 표기 정정" 금지.

## 라이선스

[MIT](LICENSE) — 기여는 같은 라이선스로 배포됩니다.
