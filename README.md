# java-escape-room

객체지향 프로그래밍 팀 프로젝트 — Java Swing 방탈출 게임

## 실행
- VS Code에서 `java-escape-room-feat-room2` 폴더를 열고 `src/common/MainFrame.java` 의 `main` 실행 (Run)
- 어느 폴더를 열어도 resources 폴더를 자동으로 찾습니다.

## 주요 기능
체력 · 제한 시간 · 난이도 · 단계별 힌트 · 현재 목표 · 인벤토리 · 메모장 · 일시정지 · 설정 · 도움말 · 저장/이어하기 · 결과 화면/등급 · 명예의 전당(랭킹) · 효과음 · 피격 연출

강의의 `Manageable` / `Manager<T>` / `Factory<T>` 구조(`src/mgr`)로 아이템 · 힌트(`resources/data/hints.txt`) · 랭킹을 관리합니다.

자세한 내용과 팀원용 연동 방법은 [기능목록.md](기능목록.md) 참고.
