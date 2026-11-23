package common;

import room1.Room1Panel;
import room2.Room2Panel;
import room3.Room3Panel;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/**
 * 게임 메인 창. 프로그램 시작점(main)도 여기에 있다.
 *
 *  화면 구성 (CardLayout)
 *   - title  : 타이틀 / 메인 메뉴
 *   - game   : [상단 HUD] + [가운데 방 화면 1280x720] + [오른쪽 사이드 메뉴]
 *   - result : 탈출 성공 / 게임 오버 결과 화면
 */
public class MainFrame extends JFrame implements GameListener {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel roomHolder = new JPanel(new BorderLayout());
    private final TitlePanel titlePanel;
    private final ResultPanel resultPanel;
    private final DamageOverlay overlay = new DamageOverlay();

    public MainFrame() {
        super("ESCAPE ROOM - 객체지향 프로그래밍 팀 프로젝트");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        GameManager gm = GameManager.getInstance();
        gm.attachFrame(this);

        // ---- 게임 화면 ----
        JPanel gameScreen = new JPanel(new BorderLayout());
        gameScreen.setBackground(Theme.BG_DARK);
        gameScreen.add(new HudPanel(), BorderLayout.NORTH);
        roomHolder.setPreferredSize(new Dimension(RoomPanel.ROOM_WIDTH, RoomPanel.ROOM_HEIGHT));
        roomHolder.setBackground(Color.BLACK);
        gameScreen.add(roomHolder, BorderLayout.CENTER);
        gameScreen.add(new SideMenuPanel(this), BorderLayout.EAST);
        installShortcuts(gameScreen);

        titlePanel = new TitlePanel(this);
        resultPanel = new ResultPanel(this);

        root.add(titlePanel, "title");
        root.add(gameScreen, "game");
        root.add(resultPanel, "result");
        setContentPane(root);
        setGlassPane(overlay);

        gm.addListener(this);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) {
                onCloseRequested();
            }
        });

        pack();
        setLocationRelativeTo(null);
        showTitle();
    }

    // ------------------------------------------------------------------
    // 화면 전환
    // ------------------------------------------------------------------

    public void showTitle() {
        titlePanel.refresh();
        cards.show(root, "title");
    }

    void showGameScreen() {
        cards.show(root, "game");
    }

    void setRoom(RoomPanel room) {
        roomHolder.removeAll();
        roomHolder.add(room, BorderLayout.CENTER);
        roomHolder.revalidate();
        roomHolder.repaint();
    }

    void showResult(GameResult result) {
        resultPanel.setResult(result);
        cards.show(root, "result");
    }

    // ------------------------------------------------------------------
    // 사이드 메뉴 / 단축키 동작
    // ------------------------------------------------------------------

    private boolean playing() {
        return GameManager.getInstance().isRunning();
    }

    public void openHint()     { if (playing()) new HintDialog(this).setVisible(true); }
    public void openMemo()     { if (playing()) new MemoDialog(this).setVisible(true); }
    public void openPause()    { if (playing()) new PauseDialog(this).setVisible(true); }
    public void openSettings() { new SettingsDialog(this).setVisible(true); }
    public void openHelp()     { new HelpDialog(this).setVisible(true); }

    public void saveGame() {
        if (!playing()) return;
        boolean ok = SaveManager.save(GameManager.getInstance());
        DialogBox.message(this, "저장", ok ? "진행 상황을 저장했습니다.\n타이틀 화면의 [이어하기]로 계속할 수 있습니다."
                                          : "저장에 실패했습니다.");
    }

    private void openFirstItem() {
        if (!playing()) return;
        java.util.List<Item> inv = GameManager.getInstance().getInventory();
        if (!inv.isEmpty()) inv.get(inv.size() - 1).view();
    }

    private void installShortcuts(JComponent c) {
        bind(c, "ESCAPE", "pause", this::openPause);
        bind(c, "H", "hint", this::openHint);
        bind(c, "M", "memo", this::openMemo);
        bind(c, "I", "item", this::openFirstItem);
        bind(c, "F5", "save", this::saveGame);
    }

    private void bind(JComponent c, String key, String name, Runnable r) {
        c.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { r.run(); }
        });
    }

    private void onCloseRequested() {
        if (playing()) {
            GameManager gm = GameManager.getInstance();
            gm.pause();
            if (DialogBox.confirm(this, "게임 종료", "진행 상황을 저장하고 종료할까요?\n(아니오를 누르면 게임으로 돌아갑니다)")) {
                SaveManager.save(gm);
                System.exit(0);
            }
            gm.resume();
        } else {
            System.exit(0);
        }
    }

    // ------------------------------------------------------------------
    // 피격 효과 (붉은 화면 + 흔들림)
    // ------------------------------------------------------------------

    @Override
    public void onDamaged() {
        if (!GameSettings.getInstance().isScreenShake()) return;
        overlay.flash();
        shake();
    }

    private void shake() {
        Point origin = getLocation();
        int[] offsets = {10, -10, 8, -8, 5, -5, 2, 0};
        Timer t = new Timer(30, null);
        final int[] i = {0};
        t.addActionListener(e -> {
            if (i[0] >= offsets.length) {
                setLocation(origin);
                t.stop();
                return;
            }
            setLocation(origin.x + offsets[i[0]++], origin.y);
        });
        t.start();
    }

    /** 오답 시 화면 전체를 잠깐 붉게 덮는 투명 레이어 */
    private static class DamageOverlay extends JComponent {
        private int alpha;
        private final Timer fade = new Timer(30, e -> {
            alpha -= 12;
            if (alpha <= 0) {
                alpha = 0;
                ((Timer) e.getSource()).stop();
                setVisible(false);
            }
            repaint();
        });

        void flash() {
            alpha = 130;
            setVisible(true);
            fade.restart();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (alpha <= 0) return;
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth(), h = getHeight();
            g2.setPaint(new RadialGradientPaint(new Point(w / 2, h / 2), Math.max(w, h) * 0.7f,
                new float[]{0f, 1f},
                new Color[]{new Color(255, 0, 0, alpha / 4), new Color(200, 0, 0, alpha)}));
            g2.fillRect(0, 0, w, h);
        }

        @Override
        public boolean contains(int x, int y) {
            return false;   // 마우스 클릭은 아래 화면으로 그대로 전달
        }
    }

    // ------------------------------------------------------------------
    // 프로그램 시작점
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");   // 윈도우 배율(125% 등)에서 화면이 잘리지 않도록
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", new Color(40, 30, 22));
            UIManager.put("ToolTip.foreground", Theme.TEXT);
            UIManager.put("ToolTip.font", Theme.font(Font.PLAIN, 13));

            GameManager gm = GameManager.getInstance();
            gm.setRoomFactories(Arrays.asList(
                Room1Panel::new,
                () -> new Room2Panel(gm::nextRoom),
                Room3Panel::new
            ));
            new MainFrame().setVisible(true);
        });
    }
}
