package common;

import javax.swing.*;
import java.awt.*;

/**
 * 게임 화면 오른쪽 사이드 메뉴.
 *  - 플레이어 정보 (이름 / 난이도)
 *  - 현재 목표 (방의 진행 상황에 맞춰 자동 갱신)
 *  - 인벤토리 (획득 아이템)
 *  - 기능 버튼: 힌트 / 메모장 / 저장 / 일시정지 / 설정 / 도움말 / 타이틀로
 */
public class SideMenuPanel extends JPanel implements GameListener {
    public static final int WIDTH = 200;

    private final JLabel playerLabel;
    private final JTextArea objectiveArea;

    public SideMenuPanel(MainFrame frame) {
        setPreferredSize(new Dimension(WIDTH, RoomPanel.ROOM_HEIGHT));
        setBackground(Theme.BG_PANEL);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 2, 0, 0, Theme.GOLD),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        setLayout(new BorderLayout(0, 10));

        // ---- 위: 플레이어 정보 + 목표 ----
        JPanel top = new JPanel(new BorderLayout(0, 6));
        top.setOpaque(false);
        playerLabel = Theme.label("", Font.BOLD, 14, Theme.TEXT);
        top.add(playerLabel, BorderLayout.NORTH);

        objectiveArea = new JTextArea();
        objectiveArea.setEditable(false);
        objectiveArea.setFocusable(false);
        objectiveArea.setLineWrap(true);
        objectiveArea.setWrapStyleWord(true);
        objectiveArea.setFont(Theme.font(Font.PLAIN, 13));
        objectiveArea.setForeground(Theme.TEXT);
        objectiveArea.setBackground(new Color(28, 21, 15));
        objectiveArea.setRows(5);
        objectiveArea.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        JPanel objBox = new JPanel(new BorderLayout());
        objBox.setOpaque(false);
        objBox.setBorder(BorderFactory.createTitledBorder(Theme.goldBorder(1), " 현재 목표 ",
            0, 0, Theme.font(Font.BOLD, 13), Theme.GOLD_LIGHT));
        objBox.add(objectiveArea);
        top.add(objBox, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- 가운데: 인벤토리 ----
        add(new InventoryPanel(), BorderLayout.CENTER);

        // ---- 아래: 기능 버튼 ----
        JPanel buttons = new JPanel(new GridLayout(0, 2, 6, 6));
        buttons.setOpaque(false);
        buttons.add(menuButton("힌트 (H)", frame::openHint));
        buttons.add(menuButton("메모 (M)", frame::openMemo));
        buttons.add(menuButton("저장", frame::saveGame));
        buttons.add(menuButton("일시정지", frame::openPause));
        buttons.add(menuButton("설정", frame::openSettings));
        buttons.add(menuButton("도움말", frame::openHelp));
        add(buttons, BorderLayout.SOUTH);

        GameManager.getInstance().addListener(this);
    }

    private JButton menuButton(String text, Runnable action) {
        JButton b = Theme.button(text, 13);
        b.setBorder(BorderFactory.createCompoundBorder(
            Theme.goldBorder(1), BorderFactory.createEmptyBorder(7, 2, 7, 2)));
        b.addActionListener(e -> action.run());
        return b;
    }

    private void refreshObjective() {
        RoomPanel room = GameManager.getInstance().getCurrentRoom();
        objectiveArea.setText(room == null ? "" : room.getObjective());
        objectiveArea.setCaretPosition(0);
    }

    @Override
    public void onRoomChanged(RoomPanel room, int index, int count) {
        PlayerStatus s = GameManager.getInstance().getStatus();
        playerLabel.setText("<html>" + s.getName() + "<br><span style='color:#b4967a;font-size:10px'>난이도: "
            + s.getDifficulty().getLabel() + "</span></html>");
        refreshObjective();
    }

    @Override
    public void onProgressChanged() {
        refreshObjective();
    }
}
