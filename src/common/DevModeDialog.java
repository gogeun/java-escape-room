package common;

import javax.swing.*;
import java.awt.*;

/**
 * 개발자 모드 (설정 창 → [개발자 모드]).
 * 비밀번호를 맞추면 게임 화면 오른쪽에 떠 있는 개발자 창이 열린다.
 *  - 퍼즐 하나 건너뛰기 (여러 번 연속으로 누를 수 있음)
 *  - 이전 방으로 / 현재 방 처음부터 / 다음 방으로
 *  - 체력 회복 / 시간 +5분
 *
 * 창은 게임을 막지 않고(모달 아님), 퍼즐을 넘기거나 방을 옮겨도 닫히지 않는다. 빈 곳을 끌어서 옮길 수 있다.
 * [닫기] 또는 [개발자 모드 잠그기]를 눌러야 닫힌다.
 *
 * 개발자 기능을 한 번이라도 쓰면 그 판은 탈출해도 랭킹에 등록되지 않는다.
 * 비밀번호를 한 번 맞추면 프로그램을 끌 때까지 다시 묻지 않는다.
 */
public class DevModeDialog extends DialogBox {
    private static final String PASSWORD = "2048";
    private static boolean unlocked;
    private static DevModeDialog instance;

    private final JLabel stateLabel;
    private final JButton skipBtn, restartBtn, prevBtn, nextBtn, healBtn, timeBtn;
    private final Timer refresher;
    private long noteUntil;   // "건너뛸 퍼즐 없음" 표시 시간

    /** 설정 창에서 호출: 잠겨 있으면 비밀번호를 먼저 묻고, 이미 열려 있으면 앞으로 가져온다 */
    public static void open(Window ignoredOwner) {
        Window frame = GameManager.getInstance().getFrame();
        if (instance != null && instance.isDisplayable()) {   // 이미 열려 있으면 (숨겨져 있어도) 다시 보여 줌
            instance.setVisible(true);
            instance.toFront();
            return;
        }
        if (unlocked || askPassword(frame)) {
            instance = new DevModeDialog(frame);
            instance.setVisible(true);
        }
    }

    private static boolean askPassword(Window owner) {
        DialogBox d = new DialogBox(owner, "개발자 모드", 400, 260);
        JPasswordField field = new JPasswordField();
        field.setFont(Theme.font(Font.BOLD, 18));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBackground(Theme.BG_PANEL);
        field.setForeground(Theme.GOLD_LIGHT);
        field.setCaretColor(Theme.GOLD_LIGHT);
        field.setBorder(BorderFactory.createCompoundBorder(Theme.goldBorder(1),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JLabel info = Theme.label("비밀번호를 입력하세요", Font.PLAIN, 14, Theme.TEXT);
        info.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel col = new JPanel(new GridLayout(0, 1, 0, 10));
        col.setOpaque(false);
        col.add(info);
        col.add(field);
        d.setContent(col);

        Runnable check = () -> {
            if (PASSWORD.equals(new String(field.getPassword()))) {
                unlocked = true;
                SoundManager.getInstance().play(SoundManager.Effect.SUCCESS);
                d.dispose();
            } else {
                SoundManager.getInstance().play(SoundManager.Effect.WRONG);
                info.setText("비밀번호가 틀렸습니다");
                info.setForeground(Theme.DANGER);
                field.setText("");
            }
        };
        field.addActionListener(e -> check.run());   // Enter 키
        d.addButton("확인", check);
        d.addButton("취소", d::dispose);
        SwingUtilities.invokeLater(field::requestFocusInWindow);
        d.setVisible(true);
        return unlocked;
    }

    private DevModeDialog(Window owner) {
        super(owner, "개발자 모드", 400, 400);
        setModalityType(ModalityType.MODELESS);      // 게임을 막지 않는 떠 있는 창
        setFocusableWindowState(false);              // 눌러도 게임 창의 키보드 포커스를 뺏지 않음
        enableDragMove();                            // 빈 곳을 끌어서 창 위치 옮기기 (돋보기를 가리지 않게)
        setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);   // 퍼즐 창이 떠 있어도 누를 수 있음
        GameManager gm = GameManager.getInstance();

        stateLabel = Theme.label(" ", Font.BOLD, 13, Theme.GOLD_LIGHT);
        stateLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel warn = Theme.label("※ 사용한 판은 랭킹에 등록되지 않습니다.", Font.PLAIN, 12, Theme.TEXT_DIM);
        warn.setHorizontalAlignment(SwingConstants.CENTER);

        skipBtn = action("퍼즐 하나 건너뛰기  ▶▶", () -> {
            if (gm.devSkipPuzzle()) GameManager.closeAllDialogs();   // 열려 있던 퍼즐 창은 닫는다 (이 창은 유지)
            else { noteUntil = System.currentTimeMillis() + 1800; refresh(); }   // 건너뛸 퍼즐이 없으면 버튼에 잠깐 표시
        });
        restartBtn = action("현재 방 처음부터", () -> gm.devJumpToRoom(gm.getRoomIndex()));
        prevBtn = action("◀ 이전 방으로", () -> gm.devJumpToRoom(gm.getRoomIndex() - 1));
        nextBtn = action("다음 방으로 ▶", () -> gm.devJumpToRoom(gm.getRoomIndex() + 1));
        healBtn = action("체력 회복", gm::devRestoreHp);
        timeBtn = action("시간 +5분", () -> gm.devAddTime(300));
        skipBtn.setFont(Theme.font(Font.BOLD, 17));

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        grid.add(prevBtn);
        grid.add(nextBtn);
        grid.add(restartBtn);
        grid.add(healBtn);
        grid.add(new JLabel());
        grid.add(timeBtn);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.add(skipBtn, BorderLayout.NORTH);
        center.add(grid, BorderLayout.CENTER);

        JPanel col = new JPanel(new BorderLayout(0, 12));
        col.setOpaque(false);
        col.add(stateLabel, BorderLayout.NORTH);
        col.add(center, BorderLayout.CENTER);
        col.add(warn, BorderLayout.SOUTH);
        setContent(col);

        addButton("개발자 모드 잠그기", () -> { unlocked = false; dispose(); });
        addButton("닫기", this::dispose);

        // 게임 창 오른쪽 아래 (목표 상자를 가리지 않게)
        if (owner != null) {
            setLocation(owner.getX() + owner.getWidth() - getWidth() - 24,
                        owner.getY() + owner.getHeight() - getHeight() - 110);
        }

        // 방 / 진행 상황이 바뀌면 표시와 버튼 상태를 자동으로 갱신
        refresher = new Timer(300, e -> refresh());
        refresher.start();
        refresh();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                refresher.stop();
                if (instance == DevModeDialog.this) instance = null;
            }
        });
    }

    private void refresh() {
        GameManager gm = GameManager.getInstance();
        MainFrame frame = gm.getFrame();
        boolean playing = gm.isRunning() && gm.getCurrentRoom() != null;
        boolean ready = playing && (frame == null || !frame.isTransitioning());
        int idx = gm.getRoomIndex(), count = gm.getRoomCount();

        if (playing) {
            RoomPanel room = gm.getCurrentRoom();
            stateLabel.setText("ROOM " + (idx + 1) + "/" + count + "  ·  " + room.getRoomName()
                + "  ·  퍼즐 " + room.getSolvedCount() + "/" + room.getTotalPuzzles()
                + (ready ? "" : "  (이동 중…)"));
        } else {
            stateLabel.setText("게임을 시작하면 사용할 수 있습니다.");
        }
        skipBtn.setEnabled(ready);
        skipBtn.setText(System.currentTimeMillis() < noteUntil ? "건너뛸 퍼즐 없음 → 다음 방으로 ▶" : "퍼즐 하나 건너뛰기  ▶▶");
        restartBtn.setEnabled(ready);
        prevBtn.setEnabled(ready && idx > 0);
        nextBtn.setEnabled(ready);
        nextBtn.setText(idx + 1 >= count ? "바로 탈출 ▶" : "다음 방으로 ▶");
        healBtn.setEnabled(ready);
        timeBtn.setEnabled(ready);
    }

    private static JButton action(String text, Runnable r) {
        JButton b = Theme.button(text, 15);
        b.addActionListener(e -> r.run());
        return b;
    }
}
