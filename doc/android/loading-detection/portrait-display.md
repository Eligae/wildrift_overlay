# 챔피언 초상화 표시 (텍스트 → 이미지)

## 결정 사항
슬롯에 챔피언명 텍스트 대신 초상화 이미지를 띄운다.
오버레이 면적이 좁아 글자 가독성 한계 → 시각 인지가 빠르다.

## 소스: 서버 avatar URL
- 서버 `/v1/champions` 응답에 이미 `avatar` URL 필드 존재.
- 클라이언트 ChampionsCache (SharedPreferences)에 hero_id → avatar URL 캐싱 중.
- Coil 의존성 이미 있어 URL → 디스크 캐시 자동.

## APK 번들 안 함
- 챔피언 ~123명 × ~10KB ≈ 1MB. 번들도 가능하지만 데이터 갱신 시 앱 빌드 필요.
- 서버 캐싱 + Coil 디스크 캐시면 첫 로드 후 오프라인도 동작.

## 매핑 — 한국명 → avatar URL
ChampionsCache가 `name_kr → (hero_id, avatar)` 형태로 저장하도록 보강.
SlotView가 championName(canonical) → ChampionsCache.lookup → Coil load.

## fallback
- 캐시 미스: 텍스트 표시 유지 (현재 동작).
- 처음 매핑되지 않은 OCR 결과: 텍스트.
