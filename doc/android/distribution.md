# 배포 — GitHub Releases (Android 우선)

Play Store / F-Droid 대신 **GitHub Releases**로 APK 직배포부터 시작.

## 결정 이유
- Play Store: `SYSTEM_ALERT_WINDOW` 정책 심사 까다로움 + 심사 지연.
- F-Droid: 빌드 reproducibility 요구로 초기 부담.
- GitHub Releases: 태그 푸시 → APK 첨부, 가장 자유롭다.

## 워크플로 (GitHub Actions)
- 트리거: `v*` 태그 푸시.
- 단계: Gradle 빌드 → 서명(release keystore) → APK 첨부 + release note 자동.
- 키스토어/패스워드: GitHub Secrets에 base64로 보관.

## 사용자 동선
[GitHub Pages 소개](../website.md) → 최신 Release 다운로드 → APK 사이드로드 ("출처 미상 앱 허용" 필요).

## 추후
다운로드 수 임계 넘으면 Play Store 동시 배포 재검토.
