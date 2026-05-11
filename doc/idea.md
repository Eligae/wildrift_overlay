# 와일드리프트 스펠체크 오버레이 — 기획 메모

## 목표

와일드리프트(Wild Rift) 플레이 중 적 챔피언의 소환사 주문(스펠) 및 궁극기 쿨다운을
화면 위에 떠 있는 오버레이로 추적할 수 있게 하는 도구.

## 핵심 제약 — 와일드리프트는 공식 클라이언트 API가 없다

PC 리그 오브 레전드와 달리 와일드리프트는 Live Client API에 해당하는 공식 경로가 없다.
따라서 **게임 상태를 자동으로 읽어오는 합법적인 방법이 없다**.
가능한 접근은 두 갈래로 나뉜다.

| 접근               | 설명                                               | 장단점                              |
| ------------------ | -------------------------------------------------- | ----------------------------------- |
| 수동 클릭형 (선택) | 사용자가 적 스펠 사용 순간 버튼을 눌러 타이머 시작 | 단순, ToS 충돌 없음, 즉시 구현 가능 |
| 자동 인식형        | 화면 캡처 + OCR/이미지 매칭으로 자동 타이머 트리거 | 구현 복잡, ToS·안티치트 리스크      |

→ **MVP는 자동 인식형**

## 플랫폼 결정 — Android 전용

사용자 선택: iOS / Android 오버레이 앱.
실제로는 **iOS는 OS 정책상 다른 앱 위에 떠 있는 시스템 오버레이가 불가능**하다.
앱스토어 정책 + iOS 샌드박싱 두 층이 동시에 막고 있어 우회 경로가 없다.

그래서:

- **MVP: Android 네이티브 앱** (`SYSTEM_ALERT_WINDOW` 권한 + `TYPE_APPLICATION_OVERLAY`)
- **iOS는 별도 트랙으로 분리** — 오버레이가 막혀 있으니 "인게임 트래커"는 포기하고
  "로딩 화면 스크린샷 분석기" 라는 완전히 다른 가치 제안으로 재구성한다.
  설계 노트는 `doc/wildrift_ios_analyzer_notes.html` 참조. 본 문서의 범위 밖.

## 기능 범위 (MVP)

- 적 챔피언 슬롯 5개 (P1 ~ P5)
- 슬롯마다:
  - 스펠 1번 버튼 (기본: 점멸 / 150s)
  - 스펠 2번 버튼 (기본: 점화 / 90s)
  - 궁극기 버튼 (기본 60s, 프리셋 순환 가능)
- 버튼 동작:
  - **탭** → 쿨다운 카운트다운 시작 (다시 탭하면 취소)
  - **롱프레스** → 스펠 종류 순환 (점멸 → 점화 → 회복 → 유체화 → 탈진 → 방어막 → 강타)
  - 궁극기 롱프레스 → 쿨다운 프리셋 순환 (60 / 75 / 90 / 105 / 120s)
- 버튼 시각 상태 4단계: `대기` / `카운트다운` / `임박(≤10s, 깜빡임)` / `완료(잠깐 강조 후 대기로 복귀)`
- 좌측 손잡이(`≡`)를 드래그해 위치 이동, 더블탭으로 접기/펴기
- 포그라운드 서비스 + 알림으로 백그라운드 유지

## 사용 시나리오 (한 라운드)

1. 게임 시작 전 오버레이를 화면 한쪽으로 배치.
2. 챔피언 선택 단계에서 슬롯 5개의 스펠을 롱프레스로 맞춤 (점멸/점화/회복/...).
3. 인게임 — 적 미드 점멸 본 순간 해당 슬롯의 점멸 버튼 탭 → 150s 카운트다운 시작.
4. 카운트다운이 10s 이하로 떨어지면 깜빡임으로 알림.
5. 완료 후 다시 대기 상태. 잘못 눌렀으면 같은 버튼 다시 탭 → 취소.
6. 게임 종료 후 손잡이 더블탭으로 접거나 권한 알림에서 종료.

## 지원하는 소환사 주문과 기본 쿨다운 (와일드리프트 기준 추정값)

| 스펠    | 라벨 | 기본 CD |
| ------- | ---- | ------- |
| Flash   | 점멸 | 150s    |
| Ignite  | 점화 | 90s     |
| Heal    | 회복 | 120s    |
| Ghost   | 유체 | 120s    |
| Exhaust | 탈진 | 120s    |
| Barrier | 방어 | 120s    |
| Smite   | 강타 | 15s     |

> 정확한 WR 패치 기준 값은 구현 시 한 번 더 검증 필요.
> 인사이트(인섭/유체화 룬 보정 등)는 v2 이후로 미룬다.

## 기술 스택 (예정)

- 언어: Kotlin
- UI: 프로그래매틱 View (Compose 없음 — 오버레이에서 LifecycleOwner / SavedStateRegistry 셋업 부담 회피)
- 오버레이: `WindowManager` + `TYPE_APPLICATION_OVERLAY`
- 백그라운드 유지: `Service` + `foregroundServiceType="specialUse"` (Android 14+)
- 최소 SDK: API 26 (Android 8.0), 타깃 SDK: 35
- 외부 의존성: AndroidX core-ktx, appcompat, Material — 그 외 없음

### 왜 RN / Flutter / KMP가 아닌가

이 앱은 **얇은 시스템 오버레이 한 화면**이 전부다. 크로스플랫폼 프레임워크의
강점(컴포넌트 재사용, 핫리로드, 빠른 UI 반복)이 여기선 거의 안 산다.

- `TYPE_APPLICATION_OVERLAY` / `FLAG_NOT_TOUCH_MODAL` / `foregroundServiceType` 등
  핵심 기능은 모두 Android Framework API. RN/Flutter에서도 결국 Kotlin 네이티브
  모듈을 직접 짜야 하고, JS/Dart 층은 그 bridge를 호출하는 얇은 껍데기가 된다.
- UI 규모: 슬롯 5 × 버튼 3 + 손잡이 1 = 16개 뷰 + 타이머. 선언형 UI 트리가
  주는 이득이 작다.
- 포그라운드 서비스가 게임과 동시에 상주해야 하므로 JS 엔진(Hermes) / Dart VM
  런타임 풋프린트는 순손해.
- iOS 트랙은 분리됐으므로 크로스플랫폼이 줄 수 있는 마지막 가치도 사라졌다.
- 핫리로드: 어차피 실기 설치 → `SYSTEM_ALERT_WINDOW` 권한 부여 → 게임 부팅
  절차를 매번 거쳐야 해서 핫리로드의 의미가 거의 없다.

## 오버레이 구현 핵심 함정

문서에 미리 박아두지 않으면 한 번씩 데이게 되는 항목들.

- **터치 인터셉트 분리**: `LayoutParams.flags |= FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE`.
  안 하면 오버레이가 게임 입력을 통째로 빨아들여 챔피언 조작 불가.
- **권한 부여 흐름**: `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` 인텐트로 사용자가
  직접 시스템 설정에서 켜야 함. 첫 실행 시 안내 화면 → 인텐트 → 복귀 후 재확인 루프.
- **포그라운드 서비스 타입**: Android 14(API 34)부터 `foregroundServiceType` 필수.
  `specialUse` 사용 시 `<property>` 태그로 `foregroundServiceTypeUseCase`도 함께 선언.
  매니페스트 누락 시 런타임 크래시.
- **알림 채널**: 포그라운드 서비스 알림은 사용자가 끌 수 있게 별도 채널로 분리.
  중요도는 `IMPORTANCE_LOW` (소리/진동 안 남).
- **틱 주기**: 1초 단위 표시면 충분. `Handler.postDelayed(1000ms)` 또는
  `Choreographer` 둘 다 가능하지만, 깜빡임 효과를 16ms 단위로 부드럽게 보이려면
  `임박` 상태에서만 Choreographer로 전환.
- **잠금화면 / 멀티윈도우**: 잠금화면 위로 떠 있을 필요는 없음 →
  `TYPE_APPLICATION_OVERLAY` 기본 동작이면 충분.
- **상태 저장**: 회전·서비스 재시작 대비해 슬롯 5개의 (스펠 종류, 카운트다운 시각)을
  SharedPreferences에 짧은 주기로 직렬화. DB 도입은 v2 이후.

## 데이터 모델 스케치

```kotlin
enum class Spell(val labelKo: String, val defaultCooldownSec: Int) {
    FLASH("점멸", 150),
    IGNITE("점화", 90),
    HEAL("회복", 120),
    GHOST("유체", 120),
    EXHAUST("탈진", 120),
    BARRIER("방어", 120),
    SMITE("강타", 15),
}

data class SlotState(
    val index: Int,                          // 0..4
    val spell1: Spell,
    val spell2: Spell,
    val ultimateCooldownSec: Int,            // 60/75/90/105/120 중 하나
    val spell1ReadyAtEpochMs: Long?,         // null이면 대기 중
    val spell2ReadyAtEpochMs: Long?,
    val ultimateReadyAtEpochMs: Long?,
)
```

오버레이는 `List<SlotState>` 한 덩어리만 들고 다닌다. 현재 시각과의 차이로 남은 시간 계산.
챔피언 식별(이름/아이콘 라벨) 같은 더 풍부한 모델은 v2 이후로 미룬다.

## ToS / 안전성 고려

- 메모리 후킹 / 패킷 스니핑 / 화면 캡처 자동화 → **하지 않는다**.
- 게임 상태는 100% 사용자 입력 기반 → 리엇 ToS와 충돌 여지 최소화.
- 안티치트(Vanguard 등)가 깔린 환경에서도 단순 화면 위 그림판 수준이라 안전.

## 결정 보류 / 차후 검토

- 자동 인식형(이미지 매칭) 도입 여부 — MVP 사용 후 재평가
- 챔피언별 정확한 궁 쿨다운 DB — 챔피언 선택 UI 도입 시 함께 추가
- 슬롯에 챔피언 이름/아이콘 라벨 부착 (v2) — 데이터 자산은 iOS 분석기 트랙과 공유 가능
- 배포 채널: Play Store(`SYSTEM_ALERT_WINDOW` 정책 까다로움) vs APK 직배포 vs F-Droid
- 다국어 — 1차는 한국어 단일. 영어는 패키지명/스토어 메타데이터에만.
- 인사이트 룬·아이오니아 장화 등 어빌리티 헤이스트 보정 — v2 이후

## 다음 단계

1. 본 문서에 대한 사용자 확인.
2. 확인되면 Android 프로젝트 스캐폴딩 — 첫 PR 단위로 끊어서:
   - **PR 1**: Gradle/AGP 셋업, 빈 `MainActivity` + `AndroidManifest`. 빌드만 되는 상태.
   - **PR 2**: `OverlayPermissionActivity` — `SYSTEM_ALERT_WINDOW` 부여 안내 + 인텐트 + 복귀 검증.
   - **PR 3**: `OverlayService` (포그라운드, 알림 채널) + 빈 오버레이 View 한 장 표시.
   - **PR 4**: 슬롯 5개 + 버튼 3개 레이아웃. 탭/롱프레스 동작은 아직 더미.
   - **PR 5**: `SlotState` 데이터 모델 + 타이머 로직 + 시각 상태 4단계.
   - **PR 6**: 손잡이 드래그/더블탭, SharedPreferences 직렬화, 마무리.
3. 실기 테스트 후 스펠 CD 값 / 레이아웃 폭 / 폰트 크기 튜닝.
4. iOS 분석기 트랙은 본 작업 완료 후 별도 일정으로 착수
   (`doc/wildrift_ios_analyzer_notes.html`).
