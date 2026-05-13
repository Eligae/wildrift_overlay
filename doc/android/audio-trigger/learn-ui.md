# 사운드 학습 UI

사용자가 게임 중 특정 소리를 들으면 앱에서 그 클립을 캡처·라벨링·저장.

## UX

1. 메인 화면 → "사운드 트리거" 진입.
2. 등록 목록 (있다면 표시: 라벨 + 길이 + 등록일).
3. "+ 새 트리거 등록" 버튼:
   - 게임에서 원하는 소리가 들릴 때까지 대기.
   - 사용자가 **"방금 들렸어요"** 버튼 탭.
   - 앱이 직전 N초(기본 4초) PCM 링버퍼에서 잘라낸 후 mini-player로 재생 시험.
   - 사용자가 시작/끝 trim slider로 정확한 구간 자름 (1~3초 권장).
   - 라벨 입력 ("매치 시작 팡파레") + 트리거 액션 선택 (match_start / match_win / match_lose / ult_ready / custom).
   - 저장 → `filesDir/sound_triggers/<id>.npfp` + 메타 JSON.

## 트리거 액션 정의
- `match_start`: `prefs.matchStartedAtMs = now`
- `match_win`: MatchRecord 저장 + verify flow
- `match_lose`: 위와 같음 (result=LOSE)
- `ult_ready`: 특정 슬롯의 spell ready 자동 (사용자가 슬롯 선택)
- `custom`: 단순 로그 출력 (디버깅용)

## 등록 화면 + 캡처 서비스 연결
- 등록 모드 진입 시 캡처 서비스에 "ring buffer 활성" broadcast.
- 사용자가 탭 시 서비스가 현재 ring buffer snapshot을 file로 dump → activity가 그걸 읽어 trim UI 표시.

## 삭제 / 비활성화
- 등록 목록에서 길게 누름 → 삭제.
- 토글로 일시 비활성화 (검출 skip).
