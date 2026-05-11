# 빌드 / 실행 액션

PR 1 시점. 코드 진척에 따라 갱신.

## 사전 요구
- JDK 17 (`JAVA_HOME` 권장)
- Android SDK + adb
- Gradle wrapper로 8.10.2 사용 (첫 빌드 시 자동 다운로드)

## 디버그 빌드
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@17 ./gradlew assembleDebug
```
산출물: `app/build/outputs/apk/debug/app-debug.apk`

## 실기 설치
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# 또는
./gradlew installDebug
```

## 클린 / 자주 쓰는 태스크
- `./gradlew clean`
- `./gradlew tasks`
- `./gradlew :app:dependencies`
