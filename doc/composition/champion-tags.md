# 챔피언 태그 (큐레이션 자산)

[synergy-rules](synergy-rules.md)와 [counter-matrix](counter-matrix.md)의 기반 데이터.

## 태그 차원
- **클래스**: tank / fighter / assassin / mage / marksman / support
- **역할**: engage / disengage / peel / dive / poke / sustain
- **CC**: hard / soft / none
- **사거리**: melee / ranged
- **데미지**: AD / AP / hybrid

## 형식
챔피언 ID(텐센트 `hero_id`) 기반 JSON:
```json
{
  "10001": {
    "class": "fighter",
    "roles": ["dive"],
    "cc": "soft",
    "range": "melee",
    "damage": "AD"
  }
}
```

## 갱신
신규 챔피언 출시마다 PR. 추천 트랙의 [champion-mapping](../recommendation/champion-mapping.md)과 같이 둘지 [pending](pending.md).
