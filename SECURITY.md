# Security Policy

## 취약점 보고

보안 취약점을 발견하면 **공개 issue로 올리지 말고** 아래 이메일로 비공개로 알려주세요.

- 연락처: <superriaco@gmail.com>
- 제목: `[security] wr_spellcheck`

수신 후 48시간 이내 1차 응답을 목표로 합니다. 패치는 영향도에 따라 합리적인 기간 안에 배포합니다.

## 보호 대상

- 클라이언트 앱이 사용하는 API 토큰 (`local.properties`의 `WR_API_TOKEN`) — 코드/저장소에 박혀선 안 됩니다.
- 서버 `.env`의 인증 키 — Railway 등 환경변수로만 주입.
- 사용자 단말의 SharedPreferences — 디스크에 영구 저장되지만 다른 앱에서 직접 읽지 못합니다.

## 알려진 제한

- v0 단계라 클라이언트-서버 통신은 HTTPS + 정적 API key 만. mTLS / OAuth / refresh token 없음.
- 캡처 PNG (`/sdcard/Android/data/.../files/capture_*.png`)는 디버그 모드에서만 생성. release 빌드에는 OFF 권장.

## 데이터 출처

서버는 `lolm.qq.com` 의 공개 hero_rank_list_v2 응답을 캐시·정규화해 제공합니다. 원본 출처와 다르거나 권리 침해가 우려되면 같은 채널로 알려주세요.
