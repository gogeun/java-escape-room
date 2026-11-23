package common;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 아직 구현되지 않은 방을 위한 임시 화면.
 * 팀원이 방을 완성하면 이 클래스 대신 RoomPanel 을 직접 상속하도록 바꾸면 된다.
 * 공통 시스템(체력 / 힌트 / 인벤토리 / 다음 방 이동)을 테스트할 수 있는 버튼을 제공한다.
 */
public class PlaceholderRoom extends RoomPanel {
    private final String roomName;
    private final String nextLabel;
    private boolean testSolved;

    public PlaceholderRoom(String roomName, String nextLabel) {
        this.roomName = roomName;
        this.nextLabel = nextLabel;
        setBackground(new Color(18, 14, 11));

        JButton wrong = Theme.button("[테스트] 오답 내기 (체력 -1)", 16);
        wrong.setBounds(440, 380, 400, 46);
        wrong.addActionListener(e -> reportWrongAnswer(roomName + "-test"));
        add(wrong);

        JButton solve = Theme.button("[테스트] 퍼즐 해결 + 아이템 획득", 16);
        solve.setBounds(440, 436, 400, 46);
        solve.addActionListener(e -> {
            testSolved = true;
            reportSolved(roomName + "-test");
            giveItem(new Item(roomName + "-key", "임시 열쇠", "테스트용 아이템입니다.", null,
                () -> DialogBox.message(this, "임시 열쇠", "테스트용 열쇠입니다.")));
        });
        add(solve);

        JButton next = Theme.button(nextLabel, 18);
        next.setBounds(440, 510, 400, 52);
        next.addActionListener(e -> clearRoom());
        add(next);
    }

    @Override public String getRoomName() { return roomName; }

    @Override public String getObjective() {
        return testSolved ? "임시 퍼즐을 해결했습니다.\n아래 버튼으로 다음 방으로 이동하세요."
                          : "이 방은 아직 구현 중입니다.\n테스트 버튼으로 공통 기능을 확인해 보세요.";
    }

    @Override public List<String> getCurrentHints() {
        return Arrays.asList("이 방은 아직 구현되지 않았습니다.", "가운데 버튼을 눌러 다음 방으로 이동하세요.");
    }

    @Override public int getTotalPuzzles() { return 1; }
    @Override public int getSolvedCount()  { return testSolved ? 1 : 0; }

    @Override public void saveState(Properties p) { p.setProperty(roomName + ".solved", String.valueOf(testSolved)); }
    @Override public void loadState(Properties p) { testSolved = Boolean.parseBoolean(p.getProperty(roomName + ".solved")); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(Theme.font(Font.BOLD, 46));
        g2.setColor(Theme.GOLD_LIGHT);
        String t = roomName;
        g2.drawString(t, (getWidth() - g2.getFontMetrics().stringWidth(t)) / 2, 230);
        g2.setFont(Theme.font(Font.PLAIN, 20));
        g2.setColor(Theme.TEXT_DIM);
        String s = "팀원 구현 예정 — RoomPanel 을 상속하여 완성하면 공통 시스템에 자동 연결됩니다.";
        g2.drawString(s, (getWidth() - g2.getFontMetrics().stringWidth(s)) / 2, 290);
    }
}
