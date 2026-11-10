# 혈월의 예배당 · Bloodmoon Chapel

Java 17 표준 라이브러리와 Swing으로 구현한 Room 3 포인트 앤 클릭 방탈출 게임입니다.
`basic.txt`와 이후 조사 위치 수정 지시를 기준으로 삼고 `구성.pdf`의 단일 JFrame, JPanel 화면 전환, 독립 진행 상태 구조를 적용했습니다.
Room 1·2와 기존 common 클래스는 변경하지 않았습니다.

다음 작업자는 [Room 3 작업 인계서](src/room3/docs/handoff.md)부터 확인하세요.
진입점, 폴더 구조의 이유, 실행·검증 명령과 아직 연결되지 않은 부분을 정리했습니다.

## 실행

- Windows 실행 파일: `src/room3/dist/windows/BloodmoonChapel/BloodmoonChapel.exe`
  - Java 런타임을 포함합니다. 배포할 때 **BloodmoonChapel 폴더 전체**를 복사하세요.
  - 설치 프로그램이 아닌, 압축해서 배포 가능한 app-image 형식입니다.
- Java 17 이상 설치 환경: `java -jar src/room3/dist/BloodmoonChapel.jar`
- 소스 빌드: `powershell -NoProfile -ExecutionPolicy Bypass -File src/room3/build.ps1`
- Windows 실행 파일 재생성: `powershell -NoProfile -ExecutionPolicy Bypass -File src/room3/package-windows.ps1`
  - JDK 17 이상이 필요합니다. 기존 `src/room3/dist/windows/BloodmoonChapel`이 있으면 다른 곳으로 옮긴 뒤 실행하세요.

## 조작과 진행

사물 위에 마우스를 올리면 돋보기 커서와 조사 영역이 나타납니다. 클릭해 조사하고,
하단 8칸 인벤토리에서 아이템을 선택합니다. 팝업은 닫기 버튼 또는 Esc로 닫습니다.
창 크기를 바꿔도 이미지 비율과 클릭 영역이 함께 조정됩니다.
게임 안에서는 풀이 설명, 레시피, 튜토리얼을 표시하지 않습니다. 아래 진행 순서와 정답은 개발 검증용 안내입니다.
현재 UI 텍스트도 임시로 숨긴 상태입니다. 닫기·앞뒤 이동·되돌리기는 아이콘으로 표시하고 아이템 이미지는 유지합니다.
UI 텍스트를 다시 표시하려면 `java -Droom3.showText=true -jar src/room3/dist/BloodmoonChapel.jar`로 실행합니다.
이미지에 있던 책상 위 책의 필기·봉인 도장 이니셜과 촛불 받침 숫자도 제거했습니다.
이미지 편집은 UI 표시 옵션과 별개로 적용되며 [편집 기록](src/room3/docs/image-text-removal.md)에 정리했습니다.

1. 왼쪽 앞 책상 조사 화면의 아래쪽 중앙 서랍을 열어 점화봉을 얻습니다.
2. 오른쪽 종이의 여섯 그림과 물약 선반을 확인합니다.
3. 책상 조사 화면 왼쪽 위 다섯 촛불을 조사하고 촛불 퍼즐을 풉니다.
4. 예배당 오른쪽 아래 의자/작은 가구 근처에 드러난 금고를 조사해 수정을 얻습니다. 장롱은 배경입니다.
5. 두 재료를 가진 상태에서 중앙 제단 조사 화면으로 이동합니다. 인벤토리에서 점화봉 또는 수정을 선택하고 제단 위를 누릅니다.
6. 지팡이가 완성되면 오른쪽 귀환문을 조사합니다. 아이템 슬롯을 먼저 선택할 필요는 없습니다.
7. 회로를 연결하면 봉인이 풀리고 탈출 화면이 나타납니다.

게임 진행과 퍼즐의 부분 입력은 화면을 닫아도 유지됩니다. 프로그램을 종료하면 초기화됩니다.
저장 파일, 제한 시간, 실패 횟수 제한은 없습니다.

<details><summary>정답과 회로 검증용 안내 (스포일러)</summary>

- 촛불: 왼쪽부터 2 → 3 → 5 → 1 → 4. 틀리면 모두 초기화됩니다.
- 금고: 오른쪽 위, 왼쪽 가운데, 아래 가운데. 순서 무관이며 선택을 취소하고 수정할 수 있습니다.
- 회로: 위쪽 왼쪽부터 행 단위로 1~16번입니다. 2, 7, 9, 11번은 고정입니다.
- 정답 경로: 1 → 2 → 3 → 7 → 6 → 5 → 9 → 10 → 11 → 12 → 16 → 오른쪽 출구.
- 초기 상태에서 회전 가능한 각 타일을 세 번씩 누르면 검증된 해답 배치가 됩니다.
  경로가 더 일찍 연결되면 즉시 성공합니다. 16개를 모두 연결할 필요는 없습니다.

</details>

## 구조와 리소스

공통 게임에는 `new Room3Panel(onComplete)`로 연결합니다. 완료 콜백, 재진입, 소리 설정과 자원 정리는
[방 연결 안내](src/room3/docs/integration.md)를 참고하세요.

Room 3에서 추가한 문서, 테스트, 실행·빌드 스크립트, 배포 파일은 모두 `src/room3/` 안에 둡니다.
`src/room3/dist/` 배포 산출물과 루트 `out/` 임시 빌드 파일은 `.gitignore`에서 제외합니다.

- `src/room3/Main.java`: 독립 실행 진입점
- `Room3Panel`: 조사 화면, 공통 퍼즐 팝업, 인벤토리 표시, 비율 기반 클릭 영역
- `GameController`, `GameState`, `InventoryManager`: 진행 조건, 아이템, 화면과 독립된 상태
- `puzzles/`: 촛불 순서, 금고 집합, 회로 회전과 BFS 연결 판정
- `AssetManager`: 클래스패스 이미지 캐시와 누락 이미지 복구 표시

이미지는 실제 저장 위치인 `resources/room3/images/`에서 읽으며 JAR 안에서는 `/room3/images/파일명`으로 접근합니다.
목록과 이미지 치수는 [리소스 매핑](src/room3/docs/assets.md)에 기록했습니다. 외부 라이브러리는 사용하지 않습니다.

물약 이미지 `resources/room3/images/potion.png`는 라벨과 글자판 자체를 제거한 이미지로 교체했습니다.
표시 단계의 검은 사각형 가리기는 제거했습니다. 편집 도구와 프롬프트는 [물약 자산 편집 기록](src/room3/docs/potion-edit.md)에 있습니다.
어린아이 그림의 원본 소실 순서는
기획 정답과 달라, 원본의 인물과 불꽃 영역을 합성해 작은 키부터 1, 2, 5, 3, 4 순서로 사라지게 했습니다.
이는 촛불 길이에 대응하면 지정된 정답 2, 3, 5, 1, 4가 됩니다. 그림 단서 PNG는 보존했습니다.
회로의 정확한 초기 배치 자료가 없어 해결 가능한 경로를 먼저 설계한 뒤 가변 타일만 회전해 초기화했습니다.

## 검증

효과음 8개는 [효과음 매핑과 검증](src/room3/docs/sounds.md)에 정리했습니다.
Java 표준 `Clip`을 EDT 밖에서 재생하며, 오디오 장치가 없어도 게임은 계속 동작합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File src/room3/test.ps1
# 실제 Swing 창과 모달 팝업을 통한 전체 이벤트 진행 검사
javac -encoding UTF-8 --release 17 -cp src/room3/dist/BloodmoonChapel.jar -d out/test src/room3/tests/room3/GuiTest.java
java -cp "src/room3/dist/BloodmoonChapel.jar;out/test" room3.GuiTest
```

기본 검사는 입장부터 탈출까지의 진행, 금고 해금·수정 획득 상태, 잘못된 입력, 아이템 중복 방지,
고정 타일, 초기화, BFS 정답 경로, JAR 리소스 로딩, Swing 생성과 누락 이미지 복구를 검사합니다.
GUI 검사는 화면 좌표로 마우스 이벤트를 전달하고 버튼을 눌러 팝업, 창 크기 변경,
아이템 선택·제단 사용과 최종 탈출까지 검증합니다. 퍼즐과 단서 화면에 직접적인 설명이 남아 있는지도 검사합니다.
미리보기 PNG는 `out/`에 생성됩니다.
