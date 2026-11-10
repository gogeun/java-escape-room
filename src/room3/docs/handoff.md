# Room 3 작업 인계 — 여기서 시작

## 현재 상태

- Java 17+, Swing, 외부 라이브러리 없음. Room 3 단독 플레이와 공통 창 삽입을 지원합니다.
- 실제 다른 방과의 연결은 아직 하지 않았습니다. 이 체크아웃의 `src/common`과 Room 1·2는 빈 클래스입니다.
- Room 1 참고 구현은 별도 `C:\java-escape-room-feat-room1`에서 확인했습니다. 해당 폴더가 없어도 Room 3 실행에는 영향이 없습니다.
- 통합 진입점은 `Room3Panel(Runnable onComplete)`입니다. `room3.Main`은 단독 실행용입니다.
- 완료 콜백은 EDT에서 패널 인스턴스당 한 번 호출됩니다. [연동 규약과 예제](integration.md)를 먼저 확인하세요.
- 공통 인벤토리, 방 사이 아이템 전달, 전체 저장·불러오기, 전체 엔딩 화면과 점수 집계 규약은 미정입니다.

## 파일을 찾는 기준

| 변경할 내용 | 파일 |
|---|---|
| 공통 창 연결·진입/이탈·조사 화면·팝업·클릭 영역 | `src/room3/Room3Panel.java` |
| 아이템 획득·퍼즐 성공·진행 조건·효과음 이벤트 | `src/room3/GameController.java` |
| 진행 플래그 | `src/room3/GameState.java` |
| 아이템 종류·선택·보유 상태 | `src/room3/InventoryManager.java` |
| 촛불·금고·회로 판정 | `src/room3/puzzles/` |
| 이미지 로딩 | `src/room3/AssetManager.java` |
| 비동기 Clip 재생·정리 | `src/room3/SoundManager.java` |
| UI 문자 표시 여부와 조작 아이콘 | `src/room3/TextDisplay.java` |
| 단독 실행 창 | `src/room3/Main.java` |
| 이미지·WAV 원본 자산 | `resources/room3/images/`, `resources/room3/sounds/` |

## 프로젝트 구조의 이유

- `src/common`, `src/room1`, `src/room2`, `src/room3`는 처음부터 있던 방별 패키지 구조입니다.
- Room 3는 다른 담당자의 파일과 충돌하지 않도록 `room3` 패키지 내부에서 구현했습니다.
- `puzzles`는 화면 코드와 정답 판정을 분리하기 위한 하위 패키지입니다.
- `docs`, `tests`, 빌드 스크립트와 `dist`를 `src/room3` 안에 둔 것은 Room 3 추가 파일을 그 안에 모아 달라는 프로젝트 관리 방침에 따른 것입니다.
  Java가 요구하는 구조는 아니며, 일반적인 소스 전용 디렉터리와는 다릅니다.
- 테스트의 `tests/room3`에서 바깥 `tests`는 테스트 소스 루트, 안쪽 `room3`는 Java 패키지입니다.
  패키지 내부 동작을 검증하기 위해 게임 코드와 같은 `package room3`를 사용합니다.
- IDE에서 게임 소스 루트는 `src`, 테스트 소스 루트는 `src/room3/tests`입니다. `src/room3` 자체를 게임 소스 루트로 설정하지 마세요.
- `src/room3/tests`와 `src/room3/dist`는 운영 코드 컴파일 대상에서 제외합니다. 제공된 빌드 스크립트에 제외 규칙이 있습니다.
- 클래스 파일·검사 미리보기는 루트 `out/`, 배포 산출물은 `src/room3/dist/`에 생성하며 둘 다 Git 제외 대상입니다.
- 추후 팀 공통 빌드 규약이 생기면 테스트·문서·배포 폴더를 분리할 수 있습니다. 그때 빌드·실행·문서 경로도 함께 수정해야 합니다.

## 유지해야 하는 게임 사양

- 책상 서랍에서 점화봉 획득. 책상에서 그림·물약·책을 조사하고 촛불 퍼즐을 진행합니다.
- 촛불 정답은 왼쪽부터 `2 → 3 → 5 → 1 → 4`. 완료 후 오른쪽 아래 의자 근처 금고가 활성화됩니다.
- 금고 정답은 0 기반 인덱스 `{2, 3, 7}`이며 순서는 무관합니다. 최초 개방 시 수정 획득.
- 제단에서 점화봉 또는 수정을 선택해 사용하면 두 재료 보유 조건을 확인해 지팡이를 만듭니다.
- 지팡이 완성 후 귀환문에서 4×4 회로 퍼즐. 고정 타일은 0 기반 `{1, 6, 8, 10}`입니다.
- 힌트·튜토리얼은 제거했습니다. UI 문자는 기본적으로 숨겨져 있으며 `-Droom3.showText=true`로 다시 표시할 수 있습니다.
- 이미지에 포함된 책의 글·촛불 숫자·물약 라벨도 제거했습니다. [자산 매핑](assets.md), [문자 제거 기록](image-text-removal.md)을 확인하세요.
- WAV 8개의 이벤트 매핑은 [효과음 문서](sounds.md)에 있습니다. 패널별 오디오를 사용하므로 다른 방의 소리를 종료하면 안 됩니다.

## 실행·검증

저장소 루트에서 실행합니다. 빌드는 JDK 17 이상이 필요합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File src/room3/test.ps1
java -jar src/room3/dist/BloodmoonChapel.jar

# 공통 창의 CardLayout에 넣어 전체 진행·완료 콜백·재진입을 검증
javac -encoding UTF-8 --release 17 -cp src/room3/dist/BloodmoonChapel.jar -d out/test src/room3/tests/room3/GuiTest.java
java -cp "src/room3/dist/BloodmoonChapel.jar;out/test" room3.GuiTest
```

최근 확인한 결과는 기본 검사 52개, 오디오 검사 36개, 전체 GUI 진행 및
CardLayout 완료 콜백·재진입·음소거·팝업/오디오 정리 통과입니다.
실제 장치에서도 WAV 8개의 START/STOP을 확인했습니다. 장치별 음질·음량은 별도 확인해야 합니다.
오디오 검사 중 나오는 `Sound unavailable: door_open (LineUnavailableException)`은 의도적으로 주입한 오류입니다.

JAR은 이미지와 WAV를 포함합니다. 공통 프로젝트에서도 `resources`가 클래스패스 루트여야 합니다.
Windows EXE 재생성은 `src/room3/package-windows.ps1`을 사용합니다. 기존 출력 폴더가 있으면 먼저 다른 곳으로 옮겨야 합니다.
생성 파일은 Git에 없을 수 있으므로 인계받은 소스에서 빌드하세요.

## 통합 담당자의 다음 작업

1. 실제 Room 1·2 코드와 공통 창을 이 저장소에 통합합니다. 현재 빈 클래스만으로는 전체 게임이 연결되지 않습니다.
2. 공통 창의 CardLayout에 Room3Panel을 한 번 생성해 등록합니다.
3. 앞 방 완료 콜백에서 Room 3를 표시하고, Room 3 완료 콜백에서 팀이 정한 다음 화면으로 이동합니다.
4. 전체 소리 설정을 `setSoundEnabled`로 전달하고, 창 종료/패널 폐기 시 `close`를 호출합니다.
5. 공통 인벤토리·저장 상태를 공유할지 합의한 뒤 인터페이스를 추가합니다. 현재는 Room 3 내부 상태에 직접 접근하지 않습니다.

다른 방의 구현과 기존 사용자 변경은 덮어쓰지 않습니다. 자산 이동 내역과 새 파일의 Git 포함 여부도 커밋 전에 확인하세요.
