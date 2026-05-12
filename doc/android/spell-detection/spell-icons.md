# 7종 스펠 reference 이미지

## 출처
1. WR 클라이언트 PNG 직접 추출 (게임 설치 디렉토리에서 풀기) — 가장 정확
2. 와일드리프트 공식 위키 / Riot 자산 — 라이선스 검토 필요
3. 직접 풀로딩 PNG에서 crop → 라벨링

## 7종
| 한국명 | enum | 영문 |
| --- | --- | --- |
| 점멸 | FLASH | Flash |
| 점화 | IGNITE | Ignite |
| 회복 | HEAL | Heal |
| 유체화 | GHOST | Ghost |
| 탈진 | EXHAUST | Exhaust |
| 방어막 | BARRIER | Barrier |
| 강타 | SMITE | Smite |

## 저장 위치
`app/src/main/res/drawable/spell_flash.png` 등. dpi mdpi 1배(약 48px). 빌드 시 자동 ID 부여.

## dHash 매칭용 precompute
앱 첫 실행 시 reference 7개의 dHash를 SharedPreferences에 캐싱 — 매칭 시 inMemory map.
