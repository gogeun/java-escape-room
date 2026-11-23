package common;

import javax.swing.*;
import java.awt.*;

/**
 * 시작 화면.
 *  - 배경 연출: BookStartPanel (1번 방 시작 연출 — 서가 → 항해 일지 → 승선 명부 서명 → 그림 속 기관실로 입장)
 *  - 아래쪽 황동 메뉴 막대: 난이도 선택 / 이어하기 / 명예의 전당 / 설정 / 도움말 / 종료
 *  - 명부에 이름을 쓰고 서명하면 그 이름과 고른 난이도로 새 게임이 시작되고, 그대로 1번 방으로 이어진다.
 *
 * BookStartPanel 은 한 번 들어가면 끝나는 연출이라, 타이틀로 돌아올 때마다 새로 만든다 (refresh).
 */
public class StartScreen extends JLayeredPane {
    private static final RoomTheme T = RoomTheme.BRASS;   // 1번 방과 같은 황동 테마

    private final MainFrame frame;
    private final JPanel menuBar;
    private final JToggleButton[] diffButtons = new JToggleButton[Difficulty.values().length];
    private final JButton continueBtn;
    private BookStartPanel book;

    public StartScreen(MainFrame frame) {
        this.frame = frame;
        setPreferredSize(new Dimension(RoomPanel.ROOM_WIDTH, RoomPanel.ROOM_HEIGHT + HudPanel.HEIGHT));
        setOpaque(true);
        setBackground(new Color(15, 18, 19));

        menuBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 7)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                RoomTheme.smooth(g2);
                int w = getWidth(), h = getHeight();
                T.chassis(g2, RoomTheme.plate(1, 1, w - 3, h - 4, 10));
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.SrcOver.derive(0.95f));
                T.enamel(g2, RoomTheme.plate(5, 5, w - 11, h - 12, 7));
                g2.setComposite(old);
                T.pin(g2, 10, h / 2 - 1, 3);
                T.pin(g2, w - 11, h / 2 - 1, 3);
                g2.dispose();
            }
        };
        menuBar.setOpaque(false);

        menuBar.add(label("난이도"));
        ButtonGroup group = new ButtonGroup();
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            Difficulty d = ds[i];
            JToggleButton b = new PlateToggle(d.getLabel());
            b.setToolTipText((d.getTimeLimitSec() / 60) + "분 · 체력 " + d.getMaxHp() + " · 힌트 " + d.getHintCount());
            b.setSelected(d == Difficulty.NORMAL);
            group.add(b);
            diffButtons[i] = b;
            menuBar.add(b);
        }
        menuBar.add(Box.createHorizontalStrut(10));

        continueBtn = new PlateButton("이어하기", () -> {
            if (!GameManager.getInstance().continueGame()) {
                DialogBox.message(this, "이어하기", "저장 파일을 불러오지 못했습니다.");
                refresh();
            }
        });
        menuBar.add(continueBtn);
        menuBar.add(new PlateButton("명예의 전당", () -> new RankingDialog(frame).setVisible(true)));
        menuBar.add(new PlateButton("설정", () -> new SettingsDialog(frame).setVisible(true)));
        menuBar.add(new PlateButton("도움말", () -> new HelpDialog(frame).setVisible(true)));
        menuBar.add(new PlateButton("종료", () -> System.exit(0)));
        add(menuBar, JLayeredPane.PALETTE_LAYER);

        refresh();
    }

    /** 타이틀 화면에 다시 들어올 때: 서가 연출을 처음부터, 이어하기 버튼 갱신 */
    public void refresh() {
        if (book != null) remove(book);
        book = new BookStartPanel(this::begin);
        book.setOnEntering(() -> menuBar.setVisible(false));   // 그림 속으로 들어가는 동안 메뉴 숨김
        add(book, JLayeredPane.DEFAULT_LAYER);
        menuBar.setVisible(true);
        boolean has = SaveManager.hasSave();
        continueBtn.setEnabled(has);
        continueBtn.setText(has ? "이어하기" : "이어하기 (없음)");
        revalidate();
        repaint();
    }

    /** 승선 명부에 서명하고 그림 속으로 들어간 뒤: 새 게임 시작 → 1번 방 */
    private void begin(String passenger) {
        if (SaveManager.hasSave() && !DialogBox.confirm(this, "새 게임",
                "저장된 진행 기록이 있습니다.\n새 게임을 시작하면 덮어쓰게 됩니다. 계속할까요?")) {
            refresh();
            return;
        }
        SaveManager.delete();
        frame.enterFromStartScreen();   // 책 속 그림이 그대로 1번 방이 되므로 어두워지는 연출 없이 이어 붙임
        GameManager.getInstance().startNewGame(passenger, selectedDifficulty());
    }

    private Difficulty selectedDifficulty() {
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) if (diffButtons[i].isSelected()) return ds[i];
        return Difficulty.NORMAL;
    }

    @Override public void doLayout() {
        int w = getWidth(), h = getHeight();
        book.setBounds(0, 0, w, h);
        Dimension pref = menuBar.getPreferredSize();
        int bw = Math.min(w - 20, pref.width + 24), bh = 52;
        menuBar.setBounds((w - bw) / 2, h - bh - 10, bw, bh);
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(Theme.font(Font.BOLD, 15));
        l.setForeground(T.textDim);
        return l;
    }

    // ---------------- 황동 판 버튼 ----------------

    private static void paintPlate(Graphics2D g2, AbstractButton b, boolean on) {
        RoomTheme.smooth(g2);
        int w = b.getWidth(), h = b.getHeight();
        boolean hover = b.getModel().isRollover() && b.isEnabled(), down = b.getModel().isPressed();
        T.chassis(g2, RoomTheme.plate(1, 1, w - 3, h - 4, 6));
        Shape face = RoomTheme.plate(4, 4, w - 9, h - 10, 4);
        g2.setPaint(new GradientPaint(0, 4, down || on ? T.enamelBottom : T.enamelTop, 0, h - 6,
            down || on ? T.enamelTop : T.enamelBottom));
        g2.fill(face);
        if (on) {
            g2.setColor(new Color(T.accentLight.getRed(), T.accentLight.getGreen(), T.accentLight.getBlue(), 70));
            g2.fill(face);
            g2.setColor(T.accentLight);
            g2.setStroke(new BasicStroke(1.6f));
            g2.draw(face);
        } else if (hover) {
            g2.setColor(new Color(T.accentLight.getRed(), T.accentLight.getGreen(), T.accentLight.getBlue(), 45));
            g2.fill(face);
        }
        g2.setFont(b.getFont());
        FontMetrics fm = g2.getFontMetrics();
        String s = b.getText();
        g2.setColor(!b.isEnabled() ? T.textDim : on || hover ? Color.WHITE : T.accentLight);
        g2.drawString(s, (w - fm.stringWidth(s)) / 2, (h - 4 + fm.getAscent() - fm.getDescent()) / 2 + (down ? 1 : 0));
    }

    private static void style(AbstractButton b) {
        b.setFont(Theme.font(Font.BOLD, 15));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        FontMetrics fm = b.getFontMetrics(b.getFont());
        b.setPreferredSize(new Dimension(fm.stringWidth(b.getText()) + 34, 38));
    }

    private static final class PlateButton extends JButton {
        PlateButton(String text, Runnable action) {
            super(text);
            style(this);
            addActionListener(e -> {
                SoundManager.getInstance().play(SoundManager.Effect.CLICK);
                action.run();
            });
        }
        @Override public void setText(String text) {
            super.setText(text);
            if (getFont() != null) style(this);
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintPlate(g2, this, false);
            g2.dispose();
        }
    }

    private static final class PlateToggle extends JToggleButton {
        PlateToggle(String text) {
            super(text);
            style(this);
            addActionListener(e -> SoundManager.getInstance().play(SoundManager.Effect.CLICK));
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            paintPlate(g2, this, isSelected());
            g2.dispose();
        }
    }
}
