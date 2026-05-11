# 디자인 가이드

LoL/와일드리프트 톤. **한눈에 보는 정보 밀도**가 핵심.

## 색상 (colors.xml)
- 배경 `#0C1117`, 패널 `#131A23`, 본문 `#E8E3D6`, 보조 `#A8A194`
- 강조 hex teal `#0AC8B9`, 헤더 gold `#C89B3C`, 경고 `#C8553D`

## 타이포
- 헤더: `serif italic`, `letterSpacing="-0.02"`, gold
- 본문: 시스템 sans, ink
- 수치: monospace, ink_soft

## 챔피언 행 (티어표 핵심)
- 좌측 **원형 아바타 48dp** (Coil load, 텐센트 avatar URL)
- 중앙 챔피언명 (16sp bold) + 강도 작은 글자
- 우측 **승률 큰 글자(gold)** + 픽/밴 작은 (monospace)
- row 간 6dp 간격, hex teal 상단 1dp accent 1~3위 강조

## Insets
root layout `android:fitsSystemWindows="true"`. theme에 statusBar/navigationBar 색상.

## 정보 밀도 원칙
- 한 화면에 챔피언 6~8개.
- 핵심 한 가지(승률) 크게, 보조 두 가지(픽/밴) 작게.
- 가독성: 간격 4~8dp, 카드 안 padding 12dp.
