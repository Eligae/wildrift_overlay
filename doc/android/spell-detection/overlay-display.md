# 오버레이 슬롯 spell 버튼: 텍스트 → 이미지

## 현재
SlotView.spell1Button / spell2Button = TextView로 "F"/"I"/"S" 등 한 글자 라벨.

## 변경
TextView 자리에 FrameLayout(ImageView + TextView).
- ready 상태일 때: 스펠 reference 이미지 + 반투명 darken
- 카운트다운 중: 이미지 + 숫자 텍스트 오버레이
- 사용자가 long-press 시 spell cycle은 기존 그대로

## 이미지 매핑
`Spell` enum에 `@DrawableRes val iconRes: Int` 필드 추가.

```kotlin
enum class Spell(val label: String, val defaultCooldownSec: Int, val iconRes: Int) {
    FLASH("F", 150, R.drawable.spell_flash),
    ...
}
```

SlotView render:
```kotlin
icon.setImageResource(state.spell1.iconRes)
text.text = remainingText  // 카운트다운 또는 빈
```

## 폴백
이미지 없거나 미로드 시 — 한 글자 라벨 표시 (현재 동작).
