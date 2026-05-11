# 텐센트 API endpoint (검증됨)

## 티어 통계
`https://mlol.qt.qq.com/go/lgame_battle_info/hero_rank_list_v2`

응답: `{result, data: {mode: {position: [...]}}}`. 챔피언 객체에 `hero_id`, `position`, `appear_rate`(픽률), `forbid_rate`(밴율), `win_rate`(승률), 각 `_percent` 버전.
- 모드 키 `0`/`1` — 의미 미정 (랭크/일반 추정).
- 라인 키 `1`~`5` — 매핑은 [champion-mapping](champion-mapping.md).

## 챔피언 메타
`https://game.gtimg.cn/images/lgamem/act/lrlib/js/heroList/hero_list.js`

응답: `{heroList: {heroId: {name, title, alias, avatar, card, roles, lane}}}`. 이름은 중국어/병음만. **한국어명은 별도 큐레이션**.

## 호출 메모
- WebFetch로 Referer 없이 200 확인.
- 서버 측 호출 무방해 보이나 헤더 변경/CDN 차단 가능성 → 모니터링 필요.
