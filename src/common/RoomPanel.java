package common;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Properties;

/**
 * 모든 방(Room1, Room2, Room3)의 부모 클래스.
 *
 * 각 방은 이 클래스를 상속(extends RoomPanel)하고 아래 추상 메서드만 구현하면
 * 체력 / 제한 시간 / 힌트 / 인벤토리 / 저장 / 진행도 시스템에 자동으로 연결된다.
 *
 *  - getRoomName()      : HUD 에 표시될 방 이름
 *  - getObjective()     : 현재 진행 상황에 맞는 목표 문구 (사이드 메뉴에 표시)
 *  - getCurrentHints()  : 현재 단계에서 줄 힌트 목록 (약한 힌트 → 강한 힌트 순서)
 *  - getTotalPuzzles() / getSolvedCount() : 방 진행도 표시용
 *  - saveState() / loadState() : 저장/불러오기 (선택)
 *
 * 방 안의 퍼즐에서는 아래 protected 도우미를 호출한다.
 *  - reportWrongAnswer(퍼즐ID) : 오답 → 체력 감소
 *  - reportSolved(퍼즐ID)      : 퍼즐 해결 → 점수 / 진행도 반영
 *  - giveItem(아이템)           : 인벤토리에 아이템 추가
 *  - clearRoom()                : 다음 방으로 이동
 */
public abstract class RoomPanel extends JPanel {

    public static final int ROOM_WIDTH = 1280;
    public static final int ROOM_HEIGHT = 720;

    protected RoomPanel() {
        setLayout(null);
        setPreferredSize(new Dimension(ROOM_WIDTH, ROOM_HEIGHT));
    }

    public abstract String getRoomName();

    public abstract String getObjective();

    public abstract List<String> getCurrentHints();

    /**
     * hints.txt 에서 힌트를 찾을 때 쓰는 방 키 (예: "room2").
     * null 이면 파일을 쓰지 않고 getCurrentHints() 만 사용한다. (선택 구현)
     */
    protected String getHintRoomKey() { return null; }

    /** 현재 진행 단계 번호 (hints.txt 의 단계 번호와 맞춤, 선택 구현) */
    protected int getHintStage() { return 0; }

    /**
     * 실제로 힌트 창에 보여 줄 힌트.
     * HintMgr(hints.txt)에 해당 방·단계 힌트가 있으면 그것을, 없으면 getCurrentHints() 를 쓴다.
     */
    public final List<String> getHints() {
        String key = getHintRoomKey();
        if (key != null) {
            List<String> fromFile = HintMgr.getInstance().getHints(key, getHintStage());
            if (!fromFile.isEmpty()) return fromFile;
        }
        return getCurrentHints();
    }

    /** 개발자 모드: 다음 퍼즐 하나를 푼 상태로 만든다. 건너뛸 퍼즐이 없으면 false (선택 구현) */
    public boolean devSkipPuzzle() { return false; }

    /** 이 방의 UI 테마 (상단 바 / 목표 상자 / 가방 / 팝업 색). 방마다 오버라이드 (선택 구현) */
    public RoomTheme getTheme() { return RoomTheme.DEFAULT; }

    public int getTotalPuzzles() { return 0; }

    public int getSolvedCount() { return 0; }

    /** 방에 처음 들어왔을 때 1번 호출됨 (선택 구현) */
    public void onEnter() {}

    /** 저장 시 호출됨. 방의 진행 상태를 props 에 기록 (선택 구현) */
    public void saveState(Properties props) {}

    /** 불러오기 시 호출됨. props 에서 방의 진행 상태를 복원 (선택 구현) */
    public void loadState(Properties props) {}

    // ------------------------------------------------------------------
    // 하위 방 클래스에서 사용하는 도우미 메서드
    // ------------------------------------------------------------------

    protected static GameManager game() {
        return GameManager.getInstance();
    }

    /** MainFrame 안에서 실행 중인지 (방 단독 테스트 main() 실행 시에는 false) */
    protected boolean isInGame() {
        return game().isRunning();
    }

    protected void reportWrongAnswer(String puzzleId) {
        game().wrongAnswer(puzzleId);
    }

    protected void reportSolved(String puzzleId) {
        game().puzzleSolved(puzzleId);
    }

    protected void giveItem(Item item) {
        game().addItem(item);
    }

    /** 목표 문구 / 진행도 갱신 요청 */
    protected void updateProgress() {
        game().notifyProgress();
    }

    protected void clearRoom() {
        game().nextRoom();
    }
}
