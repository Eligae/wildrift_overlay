# 데이터 출처

[tencent-api](tencent-api.md) 두 endpoint 검증 완료 → **옵션 A 사실상 채택**.

## A. 텐센트 백엔드 fetch (권장)
- endpoint 검증됨 → 역공학 부담 없음.
- 응답이 깔끔한 JSON.
- **남은 우려**: 중국 서버 메타 / 비공식 API의 변경 위험 / 챔피언 이름 중국어·병음만 ([champion-mapping](champion-mapping.md) 필요).

## B. op.gg / u.gg (폴백)
A가 중국 메타라 신뢰도 떨어지면 한국 서버 소스로 보강.

## C. 큐레이션 JSON (폴백)
모든 외부 소스 실패 시 사람이 직접 정리한 JSON.

## 단계
1. **v1.x 시작**: A 단독 + 챔피언 매핑 큐레이션.
2. **신뢰도 검증 후**: B로 보강 또는 교체 검토.
