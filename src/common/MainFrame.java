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
 *   - title  : 시작 화면 (StartScreen: 1번 방의 서가 · 항해 일지 연출 + 메뉴)
 *   - game   : [상단 바 (상태 + 기능 버튼)] + [방 화면 1280x720]
 *              방 화면 위에는 목표 상자(오른쪽 위), 가방(아래 가운데), 방 이동 연출이 겹쳐 그려진다.
 *   - result : 탈출 성공 / 게임 오버 결과 화면
 *
 *  UI 색과 재질은 현재 방의 RoomTheme 에 따라 바뀐다.
 */
public class MainFrame extends JFrame implements GameListener {

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final JPanel roomHolder = new JPanel(new BorderLayout());
    private final ObjectiveBox objectiveBox = new ObjectiveBox();
    private final InventoryDock inventoryDock = new InventoryDock();
    private final RoomTransition transition = new RoomTransition();
    private final StartScreen startScreen;   // 시작 화면 (1번 방의 서가 · 항해 일지 연출)
    private boolean seamlessEntry;           // 시작 화면에서 책 속 그림으로 바로 들어온 경우
    private final ResultPanel resultPanel;
    private final DamageOverlay overlay = new DamageOverlay();
    private RoomPanel shownRoom;

    public MainFrame() {
        super("ESCAPE ROOM - 객체지향 프로그래밍 팀 프로젝트");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        GameManager gm = GameManager.getInstance();
        gm.attachFrame(this);

        // ---- 게임 화면: 상단 바 + 방 화면(겹친 레이어) ----
        JPanel gameScreen = new JPanel(new BorderLayout());
        gameScreen.setBackground(Color.BLACK);
        gameScreen.add(new HudPanel(this), BorderLayout.NORTH);

        JLayeredPane stage = new JLayeredPane();
        stage.setPreferredSize(new Dimension(RoomPanel.ROOM_WIDTH, RoomPanel.ROOM_HEIGHT));
        roomHolder.setBackground(Color.BLACK);
        roomHolder.setBounds(0, 0, RoomPanel.ROOM_WIDTH, RoomPanel.ROOM_HEIGHT);
        transition.setBounds(0, 0, RoomPanel.ROOM_WIDTH, RoomPanel.ROOM_HEIGHT);
        stage.add(roomHolder, JLayeredPane.DEFAULT_LAYER);
        stage.add(objectiveBox, JLayeredPane.PALETTE_LAYER);
        stage.add(inventoryDock, JLayeredPane.PALETTE_LAYER);
        stage.add(transition, JLayeredPane.POPUP_LAYER);
        gameScreen.add(stage, BorderLayout.CENTER);
        installShortcuts(gameScreen);

        startScreen = new StartScreen(this);
        resultPanel = new ResultPanel(this);

        root.add(startScreen, "title");
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
        transition.stop();
        Theme.apply(RoomTheme.BRASS);   // 시작 화면은 1번 방과 같은 황동 테마
        shownRoom = null;
        startScreen.refresh();
        cards.show(root, "title");
    }

    void showGameScreen() {
        cards.show(root, "game");
    }

    /**
     * 방 교체 + 방 이동 연출.
     *  - 2번 방(왕실) → 3번 방(혈월) : 지하 통로를 내려가는 연출 (그동안 제한 시간 멈춤)
     *  - 그 밖의 이동 / 게임 시작 / 이어하기 : 어두워졌다 밝아지는 연출
     *  - 끝나면 화면 아래에 도착한 장소 이름이 뜬다
     */
    void setRoom(RoomPanel room) {
        GameManager gm = GameManager.getInstance();
        RoomPanel previous = shownRoom;   // 게임 중 방 이동일 때만 값이 있음 (타이틀/결과 화면을 거치면 null)
        java.awt.image.BufferedImage snapshot = previous == null ? null : snapshot(roomHolder);

        Theme.apply(room.getTheme());
        shownRoom = room;
        roomHolder.removeAll();
        roomHolder.add(room, BorderLayout.CENTER);
        roomHolder.revalidate();
        roomHolder.repaint();
        setHudOverlaysVisible(true);

        String place = room.getRoomName();
        String detail = "ROOM " + (gm.getRoomIndex() + 1) + " / " + gm.getRoomCount()
            + "   ·   " + gm.getStatus().getName() + "   ·   " + gm.getStatus().getDifficulty().getLabel();
        if (previous != null && previous.getTheme() == RoomTheme.ROYAL && room.getTheme() == RoomTheme.BLOODMOON) {
            gm.pause();
            transition.passage(snapshot, previous.getTheme(), room.getTheme(), place,
                "성의 지하 깊은 곳, 붉은 달빛이 스며드는 예배당", gm::resume);
        } else if (previous == null && seamlessEntry) {
            transition.caption(place, detail, room.getTheme());   // 책 속 그림이 그대로 1번 방이 됨
        } else {
            transition.fade(snapshot, place, detail, room.getTheme());
        }
        seamlessEntry = false;
        getContentPane().repaint();
    }

    /** 시작 화면(승선 명부 서명)에서 바로 1번 방으로 들어갈 때 호출: 어두워지는 연출 없이 이어 붙인다 */
    void enterFromStartScreen() { seamlessEntry = true; }

    /** 방 이동 연출 중인지 (개발자 모드 창이 이 동안 버튼을 잠시 막는다) */
    public boolean isTransitioning() { return transition.isBlocking(); }

    /** 방 안의 연출(예: 1번 방 승강기 출발) 동안 목표 상자와 가방을 숨길 때 사용 */
    public void setHudOverlaysVisible(boolean visible) {
        objectiveBox.setVisible(visible);
        inventoryDock.setVisible(visible);
    }

    private static java.awt.image.BufferedImage snapshot(JComponent c) {
        int w = Math.max(1, c.getWidth()), h = Math.max(1, c.getHeight());
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        c.paint(g);
        g.dispose();
        return img;
    }

    void showResult(GameResult result) {
        transition.stop();
        shownRoom = null;
        resultPanel.setResult(result);
        cards.show(root, "result");
    }

    // ------------------------------------------------------------------
    // 상단 바 버튼 / 단축키 동작
    // ------------------------------------------------------------------

    private boolean playing() {
        return GameManager.getInstance().isRunning() && !transition.isBlocking();   // 방 이동 연출 중에는 조작 막기
    }

    public void openHint()     { if (playing()) new HintDialog(this).setVisible(true); }
    public void openMemo()     { if (playing()) new MemoDialog(this).setVisible(true); }
    /** ESC: 게임 중이면 일시정지 + 설정 창 */
    public void openPause()    { if (playing()) new SettingsDialog(this).setVisible(true); }
    /** 상단 바 [설정]: 게임 중이면 일시정지되고 설정 창이 열린다 */
    public void openSettings() {
        if (GameManager.getInstance().isRunning() && !playing()) return;   // 방 이동 연출 중
        new SettingsDialog(this).setVisible(true);
    }
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
        if (System.getProperty(SCALE_APPLIED) == null && relaunchFitToScreen(args)) return;
        SwingUtilities.invokeLater(() -> {
            UIManager.put("ToolTip.background", new Color(40, 30, 22));
            UIManager.put("ToolTip.foreground", Theme.TEXT);
            UIManager.put("ToolTip.font", Theme.font(Font.PLAIN, 13));

            GameManager gm = GameManager.getInstance();
            gm.setRoomFactories(Arrays.asList(
                () -> new Room1Panel(gm.getStatus().getName(), gm::nextRoom),
                () -> new Room2Panel(gm::nextRoom),
                () -> new Room3Panel(gm::nextRoom)
            ));
            new MainFrame().setVisible(true);
        });
    }

    // ------------------------------------------------------------------
    // 화면 크기에 맞춰 창 전체 확대
    //  게임 화면은 1280 x 784 기준으로 만들어져 있다. 큰 모니터에서는 작게 보이므로
    //  화면에 꽉 차게 들어가는 배율(예: 1.25배)로 방 · 글꼴 · 팝업까지 한꺼번에 키운다.
    //  자바는 확대 배율(sun.java2d.uiScale)을 프로그램 시작 때만 정할 수 있어서,
    //  화면 크기를 잰 뒤 그 배율을 붙여 같은 프로그램을 한 번 다시 실행한다.
    // ------------------------------------------------------------------

    private static final String SCALE_APPLIED = "escape.uiScale.applied";
    /** 최대 확대 배율 (더 키우면 화면 그리기가 무거워지므로 1.4배까지) */
    private static final double MAX_SCALE = 1.4;

    /** 화면(작업 표시줄 제외)에 창이 꽉 차게 들어가는 배율. 1.0 ~ MAX_SCALE, 0.05 단위 */
    static double fitScale(double screenW, double screenH, double titleBarH) {
        double baseW = RoomPanel.ROOM_WIDTH, baseH = RoomPanel.ROOM_HEIGHT + HudPanel.HEIGHT;
        double s = Math.min(screenW * 0.98 / baseW, (screenH - titleBarH) / baseH);
        s = Math.floor(s * 20) / 20.0;
        return Math.max(1.0, Math.min(MAX_SCALE, s));
    }

    /** 배율을 바꿔 다시 실행했으면 true (이 프로세스는 새 창이 닫힐 때까지 기다렸다가 끝난다) */
    private static boolean relaunchFitToScreen(String[] args) {
        try {
            if (GraphicsEnvironment.isHeadless()) return false;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            double current = ge.getDefaultScreenDevice().getDefaultConfiguration().getDefaultTransform().getScaleX();
            Rectangle usable = ge.getMaximumWindowBounds();                 // 현재 배율 기준 크기
            double physW = usable.width * current, physH = usable.height * current;
            double target = fitScale(physW, physH, 34 * current);
            if (Math.abs(target - current) < 0.03) return false;             // 이미 알맞은 배율 → 그대로 실행

            java.util.List<String> cmd = new java.util.ArrayList<>();
            cmd.add(java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString());
            cmd.add("-Dsun.java2d.uiScale=" + target);
            cmd.add("-D" + SCALE_APPLIED + "=true");
            for (String key : new String[]{"file.encoding", "stdout.encoding", "stderr.encoding",
                                           "skyborne.mute", "room3.showText"}) {
                String v = System.getProperty(key);
                if (v != null) cmd.add("-D" + key + "=" + v);
            }
            cmd.add("-cp");
            cmd.add(System.getProperty("java.class.path"));
            cmd.add(MainFrame.class.getName());
            cmd.addAll(java.util.Arrays.asList(args));
            System.out.println("[화면] 배율 " + target + "배로 실행합니다.");

            Process game = new ProcessBuilder(cmd).inheritIO().start();
            Runtime.getRuntime().addShutdownHook(new Thread(game::destroy));
            System.exit(game.waitFor());
            return true;
        } catch (Exception e) {
            System.out.println("[화면] 배율 조정 없이 실행합니다: " + e);
            return false;
        }
    }
}
