# Room 3 효과음

`resources/room3/sounds/`의 실제 WAV 8개를 클래스패스 `/room3/sounds/이름.wav`로 로딩합니다.
전체가 44,100 Hz, 16-bit signed PCM, mono 형식이며 현재 JAR 패키징에 포함됩니다.

| 파일 | 연결 메서드 | 재생 조건 |
|---|---|---|
| candle_light.wav | GameController.light | 점화봉 보유, 미완료 촛불의 올바른 점화마다 |
| candle_fail.wav | GameController.light | 잘못된 순서로 불꽃이 초기화될 때 |
| safe_click.wav | GameController.toggleSafe | 선택/해제가 실제 반영될 때. 선택 한도 초과·잠금·완료 후 클릭 제외 |
| safe_open.wav | GameController.toggleSafe | 최초 정답 집합 완성 시 한 번 |
| item_get.wav | GameController.obtainRod / toggleSafe | 점화봉·수정 최초 획득 시 각각 한 번 |
| staff_complete.wav | GameController.combine | 제단의 아이템 사용으로 조합 성공 시 한 번 |
| circuit_rotate.wav | GameController.rotateCircuit | 회전 성공 시. 고정 타일·완료 후 동작 제외 |
| door_open.wav | GameController.rotateCircuit | 회로 최초 완성 시 한 번 |

`SoundManager.play`는 요청만 전달합니다. 리소스 읽기, 오디오 장치 열기와 Clip 조작은
단일 daemon 오디오 작업 스레드에서 수행합니다. 같은 효과음은 하나의 Clip을 재사용하고
재생 위치를 되돌리므로 같은 소리의 복사본이 겹치지 않습니다. 처리 대기 중 같은 이름의 요청은 합칩니다.
서로 다른 효과음은 동시 재생할 수 있어 금고 개방음과 수정 획득음이 모두 들립니다.

Clip은 마지막 사용 후 10초 이상(음 길이가 더 길면 재생 길이+1초) 지나면 닫습니다.
Room3Panel은 패널별 SoundManager를 소유합니다. 다른 방으로 전환하거나 창을 닫으면 해당 패널의
오디오를 비동기로 정리하고 재진입 시 다시 생성합니다. 호환용 정적 SoundManager에는 JVM 종료 훅도 등록합니다.
파일 누락·장치 오류 시 해당 효과음만 현재 실행에서 비활성화하고 오류를 표준 오류에 한 번 기록합니다.
게임 UI나 진행 상태는 영향을 받지 않습니다.

## 검증

`src/room3/test.ps1`은 기존 진행 검사와 `SoundTest`를 함께 실행합니다.
`SoundTest`는 이벤트 기록과 가상 Clip으로 효과음 매핑, 중복 방지, 고정 타일 무음,
EDT 비차단, 재생 반복, 자원 정리와 오디오 장치 오류 복구를 검사합니다.
테스트 중 출력되는 `Sound unavailable: door_open (LineUnavailableException)`은 의도적으로 주입한 오류입니다.

실제 출력 장치 테스트는 다음과 같습니다. 소리가 실제로 재생됩니다.

```powershell
javac -encoding UTF-8 --release 17 -cp src/room3/dist/BloodmoonChapel.jar -d out/test src/room3/tests/room3/SoundDeviceTest.java
java -cp "src/room3/dist/BloodmoonChapel.jar;out/test" room3.SoundDeviceTest
```

장치가 있으면 8개 Clip의 START/STOP 완료를 확인합니다. 없으면 하드웨어 검증 불가를 명시합니다.
자동 검증은 사용자가 스피커로 들은 음질이나 음량까지 판단하지 않습니다.
