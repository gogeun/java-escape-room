package room1;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.function.BiConsumer;
import javax.swing.*;
import room1.RoomProgress.Command;
import room1.RoomProgress.Item;
import room1.RoomProgress.Response;
import room1.RoomProgress.Station;

/** Direct manipulation of the two instruments. Input and artwork share the same coordinates. */
final class MechanismPanel extends JPanel {
    private static final int WIDTH = 570, HEIGHT = 232;
    private static final int WHEEL_X = 153, WHEEL_Y = 115, WHEEL_RADIUS = 70;
    private final Station station;
    private final RoomProgress state;
    private final PressureInstrument regulator;
    private final ShutdownInstrument shutdown;
    private final JButton mount;
    private final BiConsumer<String, String> feedback;
    private final Timer settling = new Timer(1000 / 30, event -> animate());
    private int selected, dragging = -1, animating = -1;
    private boolean holdingWheel, wheelTracking, rejected;
    private double lastAngle, dragOrigin, travel, springFrom, springTo;
    private long releasedAt;
    private final long[] trippedAt = new long[ShutdownInstrument.COUNT];

    MechanismPanel(Station station, RoomProgress state, BiConsumer<String, String> feedback) {
        if (station != Station.PRESSURE && station != Station.STOP) {
            throw new IllegalArgumentException("A mechanical station is required.");
        }
        this.station = station;
        this.state = state;
        this.feedback = feedback;
        regulator = station == Station.PRESSURE ? new PressureInstrument() : null;
        shutdown = station == Station.STOP ? new ShutdownInstrument() : null;
        setLayout(null);
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setMinimumSize(new Dimension(220, 96));
        setOpaque(false);
        setFocusable(true);
        getAccessibleContext().setAccessibleName(station.name);
        mount = station == Station.PRESSURE && state.has(Item.HANDLE) && !state.installed(Item.HANDLE)
                ? mountButton() : null;
        if (mount != null) add(mount);
        updateDescription();
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (SwingUtilities.isLeftMouseButton(e)) grab(local(e)); }
            @Override public void mouseDragged(MouseEvent e) { move(local(e)); }
            @Override public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                move(local(e));
                release();
                hover(local(e));
            }
            @Override public void mouseMoved(MouseEvent e) { hover(local(e)); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        bind("LEFT", () -> adjust(-1));
        bind("RIGHT", () -> adjust(1));
        bind("released SPACE", this::keyCommit);
        bind("released ENTER", this::keyCommit);
        addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e) { repaint(); }
        });
    }

    private void updateDescription() {
        String description;
        if (station == Station.STOP) {
            description = state.ready(station) || state.solved() == 2
                    ? "각 장치를 표시된 방향으로 끌고 놓으세요. 좌우 방향키로 선택하고 스페이스로 작동합니다."
                    : lockReason();
        } else if (!state.installed(Item.HANDLE)) {
            description = state.has(Item.HANDLE) ? "빈 축을 눌러 밸브 손잡이를 끼우세요."
                    : "손잡이가 빠져 있다. 축에 정비 작업대의 공구 문장이 새겨져 있다.";
        } else {
            description = "손잡이를 원형으로 끌고 놓으세요. 좌우 방향키로 돌리고 스페이스로 고정할 수 있습니다.";
        }
        getAccessibleContext().setAccessibleDescription(description);
    }

    private JButton mountButton() {
        JButton button = new JButton();
        button.getAccessibleContext().setAccessibleName("손잡이 끼우기");
        button.setToolTipText("손잡이 끼우기");
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.getModel().addChangeListener(e -> repaint());
        button.addActionListener(e -> {
            if (state.install(station)) {
                button.setVisible(false);
                updateDescription();
                feedback.accept("밸브 축에 손잡이를 끼웠다.", "unlock");
                requestFocusInWindow();
                repaint();
            }
        });
        return button;
    }

    @Override public void doLayout() {
        if (mount == null) return;
        double s = scale();
        int x = (int)Math.round((getWidth() - s * WIDTH) / 2 + s * (WHEEL_X - 68));
        int y = (int)Math.round((getHeight() - s * HEIGHT) / 2 + s * (WHEEL_Y - 65));
        mount.setBounds(x, y, (int)Math.round(136 * s), (int)Math.round(130 * s));
    }

    private void bind(String key, Runnable action) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private double scale() { return Math.min(getWidth() / (double) WIDTH, getHeight() / (double) HEIGHT); }

    private Point2D local(MouseEvent e) {
        double s = Math.max(0.001, scale());
        return new Point2D.Double((e.getX() - (getWidth() - s * WIDTH) / 2) / s,
                (e.getY() - (getHeight() - s * HEIGHT) / 2) / s);
    }

    private boolean onWheel(Point2D p) {
        double radius = p.distance(WHEEL_X, WHEEL_Y);
        return radius >= 18 && radius <= WHEEL_RADIUS + 12;
    }

    private int hitControl(Point2D p) {
        for (int i = 0; i < ShutdownInstrument.COUNT; i++) {
            if (state.engaged(ShutdownInstrument.control(i))) continue;
            if (i == animating && springTo == 0 && sinceRelease() < .24) continue;
            if (nearGrip(p, ShutdownInstrument.grip(i, position(i))) ||
                    (trippedAt[i] != 0 && nearGrip(p, ShutdownInstrument.grip(i, 0)))) return i;
        }
        return -1;
    }

    private static boolean nearGrip(Point2D p, Point2D grip) {
        return Math.abs(p.getX() - grip.getX()) <= 31 && Math.abs(p.getY() - grip.getY()) <= 23;
    }

    private void hover(Point2D p) {
        boolean hot = state.ready(station) && (station == Station.PRESSURE ? onWheel(p) : hitControl(p) >= 0);
        setCursor(Cursor.getPredefinedCursor(hot ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void grab(Point2D p) {
        requestFocusInWindow();
        if (!state.ready(station)) return;
        if (station == Station.PRESSURE) {
            holdingWheel = onWheel(p);
            wheelTracking = holdingWheel;
            lastAngle = Math.atan2(p.getY() - WHEEL_Y, p.getX() - WHEEL_X);
        } else {
            int hit = hitControl(p);
            if (hit < 0) return;
            double start = position(hit);
            if (trippedAt[hit] != 0 && nearGrip(p, ShutdownInstrument.grip(hit, 0))) start = 0;
            trippedAt[hit] = 0;
            animating = -1;
            settling.stop();
            selected = dragging = hit;
            dragOrigin = axis(p, ShutdownInstrument.control(hit)) - start * distance(ShutdownInstrument.control(hit));
            travel = start;
        }
        repaint();
    }

    private void move(Point2D p) {
        if (holdingWheel) {
            double r = p.distance(WHEEL_X, WHEEL_Y);
            double angle = Math.atan2(p.getY() - WHEEL_Y, p.getX() - WHEEL_X);
            if (r < 15 || r > WHEEL_RADIUS + 62) { wheelTracking = false; return; }
            if (wheelTracking) {
                double delta = angle - lastAngle;
                state.turn(Math.atan2(Math.sin(delta), Math.cos(delta)));
            }
            lastAngle = angle;
            wheelTracking = true;
        } else if (dragging >= 0) {
            Command command = ShutdownInstrument.control(dragging);
            travel = Math.max(0, Math.min(1, (axis(p, command) - dragOrigin) / distance(command)));
        } else return;
        repaint();
    }

    private void release() {
        if (holdingWheel) {
            holdingWheel = false;
            commitWheel();
        } else if (dragging >= 0) {
            int index = dragging;
            dragging = -1;
            commitControl(index, travel, travel >= .78);
        }
        repaint();
    }

    private static double axis(Point2D p, Command c) {
        if (c == Command.AXIS) return p.getX();
        return c == Command.COOL ? -p.getY() : p.getY();
    }

    private static double distance(Command c) { return c == Command.AXIS ? 58 : 64; }

    private void adjust(int direction) {
        if (!state.ready(station) || dragging >= 0 || holdingWheel) return;
        if (station == Station.PRESSURE) state.turn(direction * Math.PI / 12);
        else selected = Math.floorMod(selected + direction, ShutdownInstrument.COUNT);
        repaint();
    }

    private void keyCommit() {
        if (!state.ready(station) || dragging >= 0 || holdingWheel) return;
        if (station == Station.PRESSURE) commitWheel();
        else if (!state.engaged(ShutdownInstrument.control(selected))) commitControl(selected, 0, true);
        repaint();
    }

    private void commitWheel() {
        if (state.releaseValve()) {
            releasedAt = System.nanoTime();
            settling.restart();
            feedback.accept("압력 50. 안전 밸브가 걸리자 증기가 걷히고 점검등이 드러났다.", "steam");
        }
    }

    private void commitControl(int index, double from, boolean commit) {
        Command command = ShutdownInstrument.control(index);
        boolean[] latched = new boolean[ShutdownInstrument.COUNT];
        for (int i=0; i<ShutdownInstrument.COUNT; i++) latched[i] = state.engaged(ShutdownInstrument.control(i));
        Response result = commit ? state.operate(command) : Response.INACTIVE;
        animating = index;
        springFrom = from;
        springTo = state.engaged(command) ? 1 : 0;
        rejected = result == Response.MISTAKE || result == Response.RESTARTED;
        releasedAt = System.nanoTime();
        if (rejected) {
            for (int i=0; i<latched.length; i++) if (latched[i]) trippedAt[i] = releasedAt;
        }
        settling.restart();
        switch (result) {
            case ENGAGED: feedback.accept(command.label + " 완료. " + state.step() + " / 4", "lever"); break;
            case FINISHED: feedback.accept("시간축 잠금 완료. 기관이 멈추고 시간 승강기의 문이 열렸다.", "unlock"); break;
            case MISTAKE: feedback.accept("회로가 원위치로 돌아갔다. 연료 레버부터 다시 시작한다. 남은 하트 " + state.hearts() + "개.", "fail"); break;
            case RESTARTED: feedback.accept("안전 차단기가 작동했다. 하트 3개로 정지 패널을 처음부터 다시 조작한다.", "fail"); break;
            default: break;
        }
    }

    private double sinceRelease() { return (System.nanoTime() - releasedAt) / 1_000_000_000.0; }

    private double position(int index) {
        if (index == dragging) return travel;
        if (index != animating) {
            if (state.engaged(ShutdownInstrument.control(index))) return 1;
            if (trippedAt[index] == 0) return 0;
            double t = Math.min(1, (System.nanoTime() - trippedAt[index]) / 280_000_000d);
            return 1 - t * t * (3 - 2 * t);
        }
        double remaining = Math.max(0, 1 - sinceRelease() / .24);
        return springTo + (springFrom - springTo) * remaining * remaining;
    }

    private void animate() {
        if (sinceRelease() >= .65) { settling.stop(); animating = -1; }
        repaint();
    }

    @Override public void removeNotify() {
        settling.stop();
        holdingWheel = false;
        dragging = animating = -1;
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        Brass.smooth(g);
        double s = scale();
        g.translate((getWidth() - s * WIDTH) / 2, (getHeight() - s * HEIGHT) / 2);
        g.scale(s, s);
        if (station == Station.PRESSURE) {
            double lockAge = releasedAt == 0 ? Double.POSITIVE_INFINITY : sinceRelease();
            boolean focused = isFocusOwner() || (mount != null &&
                    (mount.isFocusOwner() || mount.getModel().isRollover()));
            regulator.paint(g, state, holdingWheel, focused, lockAge);
        } else {
            shutdown.paint(g, state, this::position, isFocusOwner() ? selected : -1,
                    dragging, animating, rejected, sinceRelease());
            if (state.solved() < 2 && !state.ready(station)) paintSafetyCover(g);
        }
        g.dispose();
    }

    private String lockReason() {
        return state.solved() == 0 ? "증기 압력이 높아 안전 덮개가 잠겨 있다."
                : "배관 벽의 점검등 아래에서 정비 표찰을 확인하세요.";
    }

    /** The smoked-glass interlock lets players see the levers before the station is available. */
    private void paintSafetyCover(Graphics2D g) {
        Shape glass = HudMetal.plate(4, 4, WIDTH-8, HEIGHT-8, 12);
        g.setPaint(new GradientPaint(0, 0, new Color(47, 66, 61, 164),
                WIDTH, HEIGHT, new Color(9, 21, 22, 199)));
        g.fill(glass);
        Shape clip = g.getClip();
        g.clip(glass);
        g.setColor(new Color(220, 235, 216, 19));
        g.fill(HudMetal.polygon(68, 0, 130, 0, 27, HEIGHT, -36, HEIGHT));
        g.setColor(new Color(238, 249, 230, 15));
        g.fill(HudMetal.polygon(140, 0, 145, 0, 42, HEIGHT, 36, HEIGHT));
        g.setClip(clip);
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(HudMetal.BRASS);
        g.draw(glass);
        HudMetal.chassis(g, HudMetal.plate(124, 78, 322, 76, 8));
        HudMetal.enamel(g, HudMetal.plate(127, 81, 316, 69, 6));
        HudMetal.pin(g, 135, 114, 3);
        HudMetal.pin(g, 435, 114, 3);
        String title = state.solved() == 0 ? "압력 안전 잠금" : "정비 표찰 확인";
        g.setColor(HudMetal.LIGHT);
        g.setFont(Brass.font(Font.BOLD, 17));
        g.drawString(title, (WIDTH - g.getFontMetrics().stringWidth(title)) / 2, 110);
        g.setColor(HudMetal.PAPER);
        g.setFont(Brass.font(Font.PLAIN, 13));
        String reason = lockReason();
        g.drawString(reason, (WIDTH - g.getFontMetrics().stringWidth(reason)) / 2, 133);
    }
}
