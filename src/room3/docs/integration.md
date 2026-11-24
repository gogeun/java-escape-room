# 공통 프로그램 연결

Room 3의 진입 컴포넌트는 `room3.Room3Panel`입니다.
Room 1과 같은 `Room3Panel(Runnable onComplete)` 생성자를 제공합니다.
공통 창·다음 화면은 호스트가 소유하며 이 패널은 별도 JFrame을 만들거나 프로그램을 종료하지 않습니다.

```java
CardLayout layout = new CardLayout();
JPanel rooms = new JPanel(layout);
Room3Panel room3 = new Room3Panel(() -> layout.show(rooms, "ending"));
rooms.add(room3, "room3");
rooms.add(endingPanel, "ending");
// 다른 방 완료 콜백 등에서 호출
layout.show(rooms, "room3");
```

- 생성·화면 전환·상태 조회·음소거 설정은 Swing EDT에서 수행합니다.
- 회로 완성 후 모달 퍼즐을 닫고 Room 3 종료 화면을 구성한 다음 EDT에서 완료 콜백을 한 번 호출합니다.
- 콜백 안에서 다음 방, 전체 엔딩 또는 결과 집계로 연결할 수 있습니다. 특정 방 이름을 하드코딩하지 않습니다.
- `new Room3Panel()`은 콜백 없는 단독 사용 방식입니다. `room3.Main`은 별도의 시연 실행 진입점입니다.
- `isCompleted()`로 완료 여부를 조회합니다. 내부 `GameController`를 공통 프로그램에서 조작할 필요가 없습니다.
- `setSoundEnabled(boolean)`, `isSoundEnabled()`로 호스트의 공통 소리 설정을 적용합니다.
- CardLayout으로 숨기거나 패널을 제거하면 Room 3 소유 효과음을 정리하고 남은 조사 팝업도 닫습니다.
- 동일 인스턴스로 재진입하면 퍼즐·인벤토리 상태는 유지되며 오디오는 다시 준비합니다. 완료 콜백은 반복되지 않습니다.
- 호스트가 방을 폐기하거나 창을 닫을 때는 `room3.close()`로 명시적으로 정리할 수도 있습니다.
- 방을 새로 시작하려면 새 패널 인스턴스를 생성합니다.

이미지는 클래스패스 `/room3/images/`, WAV는 `/room3/sounds/`를 사용합니다.
통합 빌드에서도 `resources` 폴더를 클래스패스 루트로 포함하세요.
현재 아이템과 진행 상태는 Room 3 내부 소유입니다. 공통 인벤토리·방 사이 아이템 전달 규약은 아직 정하지 않았습니다.
공통 `MainFrame`이나 다른 방의 코드는 이번 변경에서 수정하지 않았습니다.
