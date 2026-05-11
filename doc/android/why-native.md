# 왜 RN / Flutter / KMP가 아닌가

이 앱은 **얇은 시스템 오버레이 한 화면**. 크로스플랫폼의 강점이 거의 안 산다.

- **결국 네이티브 모듈**: `TYPE_APPLICATION_OVERLAY` / `FLAG_NOT_TOUCH_MODAL` / `foregroundServiceType` 모두 Android API. JS/Dart 층은 bridge 호출 껍데기.
- **UI 규모**: 슬롯 5 × 버튼 3 + 손잡이 = 16개 뷰. 선언형 UI 이득 작음.
- **런타임 풋프린트**: 포그라운드 서비스가 게임과 상주 → JS 엔진/Dart VM 무게 순손해.
- **iOS 분리됨**: 크로스플랫폼이 줄 마지막 가치도 사라짐.
- **핫리로드 안 산다**: 실기 설치 → 권한 부여 → 게임 부팅 사이클이 필수.
