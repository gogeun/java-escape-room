package room1;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import common.RoomPanel;
import room1.RoomProgress.Item;
import room1.RoomProgress.Station;

/**
 * The team host supplies the passenger's name and the transition to Room 2.
 * [통합] common.RoomPanel 을 상속해 공통 시스템(체력 / 힌트 / 인벤토리 / 진행도)과 연결된다.
 */
public final class Room1Panel extends RoomPanel {
    private static final double LATCH_HOLD = .55, CONTROLS_FADE = .42;
    private static final double DRIVE_AT = LATCH_HOLD + LiftJourney.DRIVE_START;
    private static final double RESULT_AT = LATCH_HOLD + LiftJourney.DURATION - .42;
    private static final double RESULT_FADE = .65, RESULT_HOLD = 1.05;
    private final Runnable onComplete;
    private final RoomProgress game = new RoomProgress();
    private final String passenger;
    private final WorkshopScene scene = new WorkshopScene(this::inspect);
    private final InventoryBar inventory = new InventoryBar(this::readItem);
    private final FlightHud hud;
    private JPanel result;
    private BufferedImage departingControls;
    private final JPanel instruments = new JPanel(null) {
        @Override public boolean contains(int x, int y) {
            for (Component part : getComponents()) {
                if (part.isVisible() && part.contains(x - part.getX(), y - part.getY())) return true;
            }
            return false;
        }
        @Override protected void paintChildren(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            if (departingControls != null || result != null) Brass.smooth(g);
            double movieTime = departure - LATCH_HOLD;
            if (departingControls != null) {
                g.setComposite(AlphaComposite.SrcOver.derive(1 - fade(movieTime / CONTROLS_FADE)));
                g.drawImage(departingControls, 0, 0, getWidth(), getHeight(), null);
            } else {
                float opacity = result == null ? fade(opening / .48)
                        : fade((departure - RESULT_AT) / RESULT_FADE);
                g.setComposite(AlphaComposite.SrcOver.derive(opacity));
                super.paintChildren(g);
            }
            g.dispose();
        }
        @Override public boolean isOptimizedDrawingEnabled() { return false; }
    };
    private final JPanel shade = new JPanel(new GridBagLayout()) {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(9, 19, 20, 145));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private final EnumMap<Station, ExplorationPanel> closeups = new EnumMap<>(Station.class);
    private final List<String> log = new ArrayList<>();
    private final Timer timer = new Timer(1000 / 30, e -> tick());
    private RoomMedia.Audio audio;
    private RoomMedia.Impact impact;
    private boolean muted = Boolean.getBoolean("skyborne.mute");
    private boolean shake = true, completionSent;
    private Sheet inspection, utility;
    private long previousTick, visibleSecond = -1;
    private double elapsed, opening, departure;

    public Room1Panel() { this("기관사", () -> {}); }
    public Room1Panel(Runnable onComplete) { this("기관사", onComplete); }

    public Room1Panel(String name, Runnable onComplete) {
        super();
        setLayout(new BorderLayout());   // [통합] 이 방은 BorderLayout 으로 무대(JLayeredPane)를 채운다
        this.onComplete = Objects.requireNonNull(onComplete);
        passenger = name == null || name.isBlank() ? "기관사" : name.strip();
        // Team hosts can enter this room directly, without preloading from the bookshelf.
        CompletableFuture.runAsync(LiftJourney::preload);
        setPreferredSize(new Dimension(1280, 760));
        hud = new FlightHud(passenger, this::openLog, this::settings);
        instruments.setOpaque(false);
        instruments.add(hud);
        instruments.add(inventory);

        shade.setOpaque(false);
        shade.setVisible(false);
        shade.addMouseListener(new MouseAdapter() {}); // Block clicks through the open sheet.
        JLayeredPane stage = new JLayeredPane() {
            @Override public void doLayout() {
                scene.setBounds(0, 0, getWidth(), getHeight());
                shade.setBounds(0, 0, getWidth(), getHeight());
                instruments.setBounds(0, 0, getWidth(), getHeight());
                layoutInstruments(getWidth(), getHeight());
                adjustSheet();
            }
        };
        stage.add(scene, JLayeredPane.DEFAULT_LAYER);
        stage.add(shade, JLayeredPane.MODAL_LAYER);
        stage.add(instruments, JLayeredPane.POPUP_LAYER);
        add(stage);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (shade.isVisible()) back();
                else if (isInGame()) game().getFrame().openPause();   // [통합] 게임 중에는 공통 일시정지
                else if (game.solved() < 2) settings();
            }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing() && !completionSent) {
                previousTick = System.nanoTime();
                if (!muted) {
                    audio = new RoomMedia.Audio();
                    if (departure >= DRIVE_AT && departure < LATCH_HOLD + LiftJourney.DURATION)
                        audio.play("elevator", departure - DRIVE_AT);
                }
                if (impact == null) impact = RoomMedia.Impact.start(!muted);
                timer.start();
            } else {
                timer.stop();
                closeAudio();
            }
        });
        remember("기관실 입장. 압력계가 85를 가리킨다.");
        if (isInGame()) {   // [통합] 게임 중에는 공통 상단 바 / 가방을 쓰므로 방 자체 계기판과 공구함은 숨김
            hud.setVisible(false);
            inventory.setVisible(false);
            muted = muted || !common.GameSettings.getInstance().isSoundOn();
        }
        refresh();
    }

    /** Previous team hosts may still include this value on their result screen. */
    public int getHintsUsed() { return 0; }
    public int getScore() { return 100 * game.solved(); }
    public long getElapsedSeconds() { return (long) elapsed; }

    private String time() {
        long seconds = getElapsedSeconds();
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }

    private void refresh() {
        int solved = game.solved();
        hud.setVitals(game.hearts(), solved);
        scene.setStopStep(game.step());
        scene.setValve(game.installed(Item.HANDLE), game.turns());
    }

    private void play(String sound) {
        if (sound != null && audio != null && !muted) audio.play(sound);
    }

    private void closeAudio() {
        if (impact != null) impact.cancel();
        scene.setImpact(1, 0);
        if (audio != null) audio.close();
        audio = null;
    }

    private void remember(String line) {
        log.add(time() + "   " + line);
    }

    private void feedback(String message, String sound) {
        reportToGame(sound);
        remember(message);
        if (inspection != null) {
            inspection.notice.setText(message);
            inspection.notice.setForeground("fail".equals(sound) ? new Color(252, 165, 139) : Brass.GREEN);
        }
        play(sound);
        if (game.solved() == 2 && audio != null && !muted) audio.prepare("elevator");
        refresh();
    }

    private static float fade(double fraction) {
        double t = Math.max(0, Math.min(1, fraction));
        return (float)(t * t * (3 - 2 * t));
    }

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - previousTick) / 1_000_000_000.0;
        previousTick = now;
        if (opening < 1) {
            opening = Math.min(1, opening + dt);
            instruments.repaint();
        }
        if (impact != null) {
            double phase = impact.position(now);
            scene.setImpact(phase, shake ? impact.strength(phase) : 0);
        }
        if (game.solved() < 2) {
            elapsed += dt;
            if (getElapsedSeconds() != visibleSecond) {
                visibleSecond = getElapsedSeconds();
                hud.setTime(time());
            }
        } else {
            double before = departure;
            departure += Math.min(dt, .15);
            if (before < LATCH_HOLD && departure >= LATCH_HOLD) fadeControls();
            if (departure >= LATCH_HOLD + CONTROLS_FADE) departingControls = null;
            if (before < DRIVE_AT && departure >= DRIVE_AT) play("elevator");
            if (result == null && departure >= RESULT_AT) showArrival();
            instruments.repaint();
        }
        double transit = Math.min(1, Math.max(0, (departure - LATCH_HOLD) / LiftJourney.DURATION));
        int visibleStage = game.solved() == 2 && departure < LATCH_HOLD ? 1 : game.solved();
        scene.setValve(game.installed(Item.HANDLE), game.turns());
        scene.update(opening, visibleStage, transit, Math.min(dt, .15));
        if (departure >= RESULT_AT + RESULT_FADE + RESULT_HOLD && !completionSent) {
            completionSent = true;
            timer.stop();
            closeAudio();
            onComplete.run();
        }
    }

    private void inspect(Station station) {
        if (game.solved() == 2) return;
        JComponent content;
        switch (station) {
            case DESK, LAMP, TUBE, WINDOW ->
                content = closeups.computeIfAbsent(station, s -> new ExplorationPanel(s, game, this::collect,
                        message -> feedback(message, null), this::play));
            case PRESSURE, STOP -> content = new MechanismPanel(station, game, this::feedback);
            case LIFT -> content = note("승강기 문이 잠겨 있다. 문 위의 표시등 두 개가 압력 기관과 정지 패널에 연결돼 있다.");
            default -> throw new IllegalStateException(station.name());
        }
        utility = null;
        inspection = new Sheet(station.name, content, station != Station.LIFT);
        displaySheet();
    }

    private void collect(Item item) {
        if (game.take(item)) {
            inventory.addItem(item);
            giveItem(new common.Item("R1-" + item.name(), item.name, item.heading + " — " + item.text,
                itemIcon(item), () -> readItem(item)));   // [통합] 공통 인벤토리에도 등록
            feedback(item.name + " 획득. 아래 인벤토리에서 다시 살펴볼 수 있다.", "item");
        }
    }

    private void readItem(Item item) {
        if (game.solved() == 2) return;
        openUtility(item.name, itemCard(item));
    }

    private JPanel itemCard(Item item) {
        JPanel card = new JPanel(new BorderLayout(20, 10));
        card.setBackground(Brass.PAPER);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JPanel illustration = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                int size = Math.min(90, Math.min(getWidth(), getHeight()));
                InventoryBar.drawItem((Graphics2D) g, item, (getWidth()-size)/2, (getHeight()-size)/2, size);
            }
        };
        illustration.setOpaque(false);
        illustration.setPreferredSize(new Dimension(94, 94));
        card.add(illustration, BorderLayout.WEST);
        JTextArea copy = prose(item.heading + "\n\n" + item.text + "\n\n" + item.note, Brass.BROWN);
        JScrollPane paper = scroll(copy, Brass.PAPER);
        card.add(paper);
        return card;
    }

    private static JTextArea prose(String message, Color color) {
        JTextArea copy = new JTextArea(message);
        copy.setFont(Brass.font(Font.PLAIN, 16));
        copy.setLineWrap(true);
        copy.setWrapStyleWord(true);
        copy.setForeground(color);
        copy.setOpaque(false);
        copy.setEditable(false);
        copy.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return copy;
    }

    private static JScrollPane scroll(JComponent view, Color background) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(background);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent note(String text) { return scroll(prose(text, Brass.CREAM), Brass.PLATE); }

    private void openLog() {
        if (game.solved() == 2) return;
        JTextArea list = prose(String.join("\n\n", log), Brass.CREAM);
        list.setCaretPosition(list.getDocument().getLength());
        openUtility("메시지 기록", scroll(list, Brass.PLATE));
    }

    private void settings() {
        if (game.solved() == 2) return;
        JPanel switches = new JPanel(new GridLayout(0, 1, 0, 9));
        switches.setOpaque(false);
        String audioLabel = "효과음   " + (muted ? "꺼짐" : "켜짐");
        JButton soundSwitch = Brass.button(audioLabel, () -> {
            muted = !muted;
            closeAudio();
            if (!muted && isShowing()) { audio = new RoomMedia.Audio(); play("lever"); }
            settings();
        });
        switches.add(soundSwitch);
        switches.add(Brass.button("입장 시 화면 진동   " + (shake ? "켜짐" : "꺼짐"), () -> {
            shake = !shake;
            settings();
        }));
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(switches, BorderLayout.NORTH);
        openUtility("설정", panel);
    }

    private void openUtility(String title, JComponent body) {
        utility = new Sheet(title, body, false);
        displaySheet();
    }

    private void back() {
        if (game.solved() == 2) return;
        if (utility != null) utility = null;
        else inspection = null;
        displaySheet();
    }

    private void layoutInstruments(int width, int height) {
        hud.setBounds(0, 0, width, 84);
        Dimension dock = inventory.sizeFor(width);
        int dockY = height - dock.height - 8;
        inventory.setBounds((width - dock.width) / 2, dockY, dock.width, dock.height);
        if (result != null) {
            int captionHeight = Math.min(146, height / 3);
            result.setBounds(0, height-captionHeight, width, captionHeight);
            result.doLayout();
        }
        int top = width < 850 ? 66 : 83;
        int bottom = height - dockY + 2;
        Insets reserved = shade.getInsets();
        if (reserved.top != top || reserved.bottom != bottom)
            shade.setBorder(BorderFactory.createEmptyBorder(top, 0, bottom, 0));
    }

    private void adjustSheet() {
        Sheet current = utility != null ? utility : inspection;
        if (current == null) return;
        int w = Math.max(1, Math.min(776, shade.getWidth() - 20));
        int idealHeight = inspection == current ? 472 : 345;
        Insets reserved = shade.getInsets();
        int h = Math.max(1, Math.min(idealHeight, shade.getHeight() - reserved.top - reserved.bottom - 8));
        Dimension size = new Dimension(w, h);
        if (!size.equals(current.getPreferredSize())) current.setPreferredSize(size);
    }

    private void displaySheet() {
        shade.removeAll();
        Sheet current = utility != null ? utility : inspection;
        scene.targetsVisible(current == null && game.solved() < 2);
        if (current != null) {
            shade.add(current);
            adjustSheet();
            SwingUtilities.invokeLater(() -> { if (current.isShowing()) current.close.requestFocusInWindow(); });
        }
        shade.setVisible(current != null);
        shade.revalidate();
        shade.repaint();
    }

    private final class Sheet extends JPanel {
        final JLabel notice = Brass.label(" ", 14, Brass.GREEN, false);
        final JButton close = Brass.button("닫기", Room1Panel.this::back);
        Sheet(String title, JComponent body, boolean hasFeedback) {
            super(new BorderLayout(0, 7));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(13, 16, 12, 16));
            JPanel top = new JPanel(new BorderLayout(8, 0));
            top.setOpaque(false);
            top.add(Brass.label(title, 21, Brass.GOLD, true));
            top.add(close, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            add(body);
            if (hasFeedback) add(notice, BorderLayout.SOUTH);
        }
        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            Brass.smooth(g);
            int w = getWidth(), h = getHeight();
            if (w > 20 && h > 16) {
                HudMetal.chassis(g, HudMetal.plate(1, 1, w-3, h-4, 12));
                g.setPaint(new GradientPaint(0, 0, new Color(45, 63, 54), w/2f, h, new Color(19, 29, 29)));
                g.fill(HudMetal.plate(4, 4, w-9, h-10, 10));
                g.setColor(new Color(121, 122, 88));
                g.drawLine(17, 7, w-18, 7);
                g.setColor(new Color(102, 105, 79));
                g.drawLine(16, 55, w-17, 55);
                HudMetal.pin(g, 9, h/2, 2);
                HudMetal.pin(g, w-10, h/2, 2);
            }
            g.dispose();
        }
    }

    private void fadeControls() {
        // Fade the sheet, its dimming layer and each HUD widget as one exposure.
        // Their opaque child painters can then finish immediately, with no ghosts on the moving camera.
        if (instruments.getWidth() > 0 && instruments.getHeight() > 0) {
            BufferedImage overlay = new BufferedImage(instruments.getWidth(), instruments.getHeight(),
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = overlay.createGraphics();
            if (shade.isVisible()) shade.printAll(g);
            instruments.printAll(g);
            g.dispose();
            departingControls = overlay;
        }
        inspection = null;
        utility = null;
        displaySheet();
        hud.setVisible(false);
        inventory.setVisible(false);
    }

    private void showArrival() {
        int length = passenger.codePointCount(0, passenger.length());
        String displayName = length > 16 ? passenger.substring(0, passenger.offsetByCodePoints(0, 15)) + "…" : passenger;
        result = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(21, 18, 18, 0),
                        0, getHeight(), new Color(21, 18, 18, 204)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
        };
        result.setOpaque(false);
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));
        JLabel destination = new JLabel("과거의 성");
        Font serif = new Font("Batang", Font.BOLD, 29);
        destination.setFont(serif.canDisplay('성') ? serif : Brass.font(Font.BOLD, 29));
        destination.setForeground(new Color(248, 235, 202));
        destination.setAlignmentX(CENTER_ALIGNMENT);
        JLabel record = Brass.label(displayName + "  ·  기관실 탈출  2 / 2  ·  " + time() + "  ·  " + getScore() + "점",
                14, new Color(238, 220, 187), false);
        record.putClientProperty("html.disable", Boolean.TRUE);
        record.setAlignmentX(CENTER_ALIGNMENT);
        result.add(Box.createVerticalGlue());
        result.add(destination);
        result.add(Box.createVerticalStrut(6));
        result.add(record);
        result.add(Box.createVerticalStrut(23));
        instruments.add(result);
        layoutInstruments(getWidth(), getHeight());
        revalidate();
        repaint();
    }

    // ------------------------------------------------------------------
    // [통합] common.RoomPanel 연결 (체력 / 힌트 / 목표 / 진행도 / 인벤토리)
    // ------------------------------------------------------------------

    private int reportedSolved;   // 공통 시스템에 이미 알린 해결 퍼즐 수

    /** feedback() 의 효과음 종류(fail 등)로 정답·오답을 판단해 공통 시스템에 알린다. */
    private void reportToGame(String sound) {
        if ("fail".equals(sound)) reportWrongAnswer("P02");   // 정지 패널 순서 오류 → 체력 -1
        while (reportedSolved < game.solved()) {
            reportedSolved++;
            reportSolved("P0" + reportedSolved);               // P01 압력 밸브, P02 정지 패널
            if (reportedSolved == 2 && isInGame()) game().getFrame().setHudOverlaysVisible(false);   // 승강기 출발 연출 동안 숨김
        }
        updateProgress();
    }

    /** 공통 인벤토리 칸에 쓸 아이콘 (InventoryBar 의 아이템 그림을 사용) */
    private static BufferedImage itemIcon(Item item) {
        BufferedImage icon = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        Brass.smooth(g);
        InventoryBar.drawItem(g, item, 0, 0, 64);
        g.dispose();
        return icon;
    }

    @Override public String getRoomName() { return "공중도시의 시간 기관실"; }

    @Override public common.RoomTheme getTheme() { return common.RoomTheme.BRASS; }   // 황동 + 비취색 UI

    @Override public String getObjective() {
        switch (getHintStage()) {
            case 1: return "기관실을 둘러보고 정비 작업대에서 쓸 만한 도구를 찾자.";
            case 2: return "밸브 손잡이를 얻었다.\n가운데 압력 기관의 빈 축에 끼워 보자.";
            case 3: return "압력계가 85를 가리킨다.\n밸브를 돌려 안전 압력으로 낮추자.";
            case 4: return "증기가 걷혔다.\n점검등을 켜서 벽에 걸린 정비 표찰을 찾아보자.";
            case 5: return "정비 표찰의 절차대로\n기관 정지 패널을 조작하자.";
            default: return "기관이 멈췄다! 시간 승강기를 타고 다음 방으로 이동한다.";
        }
    }

    @Override public List<String> getCurrentHints() {
        // hints.txt 의 room1 힌트를 못 읽었을 때 쓰는 기본 힌트
        return Arrays.asList("화면의 기계와 물건을 클릭해 조사해 보세요.",
            "아래 인벤토리의 아이템을 클릭하면 내용을 다시 읽을 수 있습니다.");
    }

    @Override protected String getHintRoomKey() { return "room1"; }

    @Override protected int getHintStage() {
        if (game.solved() >= 2) return 6;
        if (game.solved() == 1) return game.has(Item.TAG) ? 5 : 4;
        if (!game.has(Item.HANDLE)) return 1;
        return game.installed(Item.HANDLE) ? 3 : 2;
    }

    /** [통합] 개발자 모드: 다음 단계 하나를 바로 진행 */
    @Override public boolean devSkipPuzzle() {
        if (game.solved() >= 2) return false;
        inspection = null;
        utility = null;
        displaySheet();
        switch (getHintStage()) {
            case 1 -> { collect(Item.HANDLE); collect(Item.CHART); }
            case 2 -> { game.devInstall(); feedback("[개발자] 밸브 축에 손잡이를 끼웠다.", "unlock"); }
            case 3 -> { game.devSolveValve(); feedback("[개발자] 압력 50. 안전 밸브가 걸리고 증기가 걷혔다.", "steam"); }
            case 4 -> collect(Item.TAG);
            case 5 -> { game.devFinishStop(); feedback("[개발자] 시간축 잠금 완료. 시간 승강기의 문이 열렸다.", "unlock"); }
            default -> { return false; }
        }
        return true;
    }

    @Override public int getTotalPuzzles() { return 2; }
    @Override public int getSolvedCount() { return game.solved(); }

    /** Standalone Room 1 preview for the team's existing Eclipse launch configuration. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("공중도시의 시간 기관실");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(new Room1Panel());
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.setSize(Math.min(1280, bounds.width), Math.min(760, bounds.height));
            frame.setMinimumSize(new Dimension(Math.min(720, bounds.width), Math.min(480, bounds.height)));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
