package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.GlyphVector;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 방탈출 컨셉의 타이틀(메인 메뉴) 화면.
 *
 *  - 돌벽 지하 감옥 배경 + 쇠사슬과 자물쇠로 봉인된 아치형 나무 문
 *  - 양옆에서 일렁이는 횃불, 위로 떠오르는 불씨, 왼쪽의 모래시계(제한 시간 상징)
 *  - 오른쪽 메뉴: 이름 / 난이도 / 새 게임 · 이어하기 · 명예의 전당 · 설정 · 도움말 · 종료
 *  - [새 게임] → 자물쇠가 풀리고 문이 열리는 연출 → 도입 스토리 → 1번 방 시작
 *
 * 모든 그림은 이미지 파일 없이 Java2D 로 직접 그린다.
 */
public class TitlePanel extends JPanel {

    private static final int W = RoomPanel.ROOM_WIDTH + SideMenuPanel.WIDTH;   // 1480
    private static final int H = RoomPanel.ROOM_HEIGHT + HudPanel.HEIGHT;       // 774

    // 문 위치
    private static final int DOOR_CX = 700;
    private static final int DOOR_W = 290;
    private static final int DOOR_TOP = 225;
    private static final int DOOR_H = 410;

    private enum State { IDLE, OPENING, STORY }

    private static final String[] STORY = {
        "차가운 돌바닥 위에서 눈을 떴다.",
        "이곳은 오래전 버려진 왕국의 성. 모든 문이 굳게 잠겨 있다.",
        "벽에 새겨진 글귀가 희미하게 빛나기 시작한다.",
        "\"세 개의 방에 숨겨진 비밀을 풀어라. 시간이 다하기 전에.\"",
        "이제, 탈출을 시작한다."
    };

    private final Random rnd = new Random();
    private final Timer anim;
    private final List<Ember> embers = new ArrayList<>();
    private final double[] flicker = {1, 1};
    private BufferedImage wallImg;
    private BufferedImage doorImg;

    private State state = State.IDLE;
    private long stateStart;
    private long frame;
    private Runnable afterOpen;
    private boolean showStory;

    private final JPanel menuBox;
    private final JTextField nameField;
    private final JRadioButton[] diffButtons = new JRadioButton[Difficulty.values().length];
    private final JButton continueBtn;

    public TitlePanel(MainFrame frame) {
        setLayout(null);
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);

        // ------------------------------------------------------------
        // 오른쪽 메뉴 상자
        // ------------------------------------------------------------
        menuBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                paintMenuPlate((Graphics2D) g, getWidth(), getHeight());
            }
        };
        menuBox.setOpaque(false);
        menuBox.setLayout(new BoxLayout(menuBox, BoxLayout.Y_AXIS));
        menuBox.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        menuBox.setBounds(1015, 170, 420, 500);

        JLabel head = Theme.label("탈출 준비", Font.BOLD, 24, Theme.GOLD_LIGHT);
        head.setAlignmentX(CENTER_ALIGNMENT);
        menuBox.add(head);
        menuBox.add(Box.createVerticalStrut(18));

        JLabel nameLabel = Theme.label("탈출자 이름", Font.BOLD, 14, Theme.GOLD);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);
        menuBox.add(nameLabel);
        menuBox.add(Box.createVerticalStrut(5));
        nameField = new JTextField("플레이어");
        nameField.setHorizontalAlignment(JTextField.CENTER);
        nameField.setFont(Theme.font(Font.BOLD, 16));
        nameField.setBackground(new Color(28, 21, 15));
        nameField.setForeground(Theme.GOLD_LIGHT);
        nameField.setCaretColor(Theme.GOLD_LIGHT);
        nameField.setBorder(BorderFactory.createCompoundBorder(Theme.goldBorder(1),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        nameField.setMaximumSize(new Dimension(260, 34));
        nameField.setAlignmentX(CENTER_ALIGNMENT);
        menuBox.add(nameField);
        menuBox.add(Box.createVerticalStrut(14));

        JPanel diffCol = new JPanel(new GridLayout(0, 1, 0, 0));
        diffCol.setOpaque(false);
        diffCol.setBorder(BorderFactory.createTitledBorder(Theme.goldBorder(1), " 난이도 ",
            0, 0, Theme.font(Font.BOLD, 14), Theme.GOLD_LIGHT));
        ButtonGroup group = new ButtonGroup();
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) {
            Difficulty d = ds[i];
            JRadioButton r = new JRadioButton("<html><b>" + d.getLabel() + "</b>  <span style='color:#a89070'>"
                + (d.getTimeLimitSec() / 60) + "분 · 체력 " + d.getMaxHp() + " · 힌트 " + d.getHintCount()
                + "</span></html>", d == Difficulty.NORMAL);
            r.setOpaque(false);
            r.setForeground(Theme.TEXT);
            r.setFont(Theme.font(Font.PLAIN, 14));
            r.setFocusPainted(false);
            r.setCursor(new Cursor(Cursor.HAND_CURSOR));
            group.add(r);
            diffButtons[i] = r;
            diffCol.add(r);
        }
        diffCol.setMaximumSize(new Dimension(360, 105));
        diffCol.setAlignmentX(CENTER_ALIGNMENT);
        menuBox.add(diffCol);
        menuBox.add(Box.createVerticalStrut(16));

        JButton newBtn = Theme.button("새 게임 — 문을 연다", 18);
        newBtn.setAlignmentX(CENTER_ALIGNMENT);
        newBtn.setMaximumSize(new Dimension(360, 46));
        newBtn.addActionListener(e -> {
            if (SaveManager.hasSave() && !DialogBox.confirm(this, "새 게임",
                    "저장된 진행 기록이 있습니다.\n새 게임을 시작하면 덮어쓰게 됩니다. 계속할까요?")) return;
            SaveManager.delete();
            String name = getPlayerName();
            Difficulty d = getDifficulty();
            startSequence(true, () -> GameManager.getInstance().startNewGame(name, d));
        });
        menuBox.add(newBtn);
        menuBox.add(Box.createVerticalStrut(10));

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 8));
        grid.setOpaque(false);
        grid.setMaximumSize(new Dimension(360, 128));
        grid.setAlignmentX(CENTER_ALIGNMENT);

        continueBtn = Theme.button("이어하기", 15);
        continueBtn.addActionListener(e -> startSequence(false, () -> {
            if (!GameManager.getInstance().continueGame()) {
                DialogBox.message(this, "이어하기", "저장 파일을 불러오지 못했습니다.");
                refresh();
            }
        }));
        JButton rankBtn = Theme.button("명예의 전당", 15);
        rankBtn.addActionListener(e -> new RankingDialog(frame).setVisible(true));
        JButton setBtn = Theme.button("설정", 15);
        setBtn.addActionListener(e -> new SettingsDialog(frame).setVisible(true));
        JButton helpBtn = Theme.button("도움말", 15);
        helpBtn.addActionListener(e -> new HelpDialog(frame).setVisible(true));
        JButton exitBtn = Theme.button("종료", 15);
        exitBtn.addActionListener(e -> System.exit(0));

        grid.add(continueBtn);
        grid.add(rankBtn);
        grid.add(setBtn);
        grid.add(helpBtn);
        menuBox.add(grid);
        menuBox.add(Box.createVerticalStrut(8));
        exitBtn.setAlignmentX(CENTER_ALIGNMENT);
        exitBtn.setMaximumSize(new Dimension(360, 36));
        menuBox.add(exitBtn);

        add(menuBox);

        // ------------------------------------------------------------
        // 스토리 건너뛰기 (클릭 / SPACE / ENTER)
        // ------------------------------------------------------------
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { skip(); }
        });
        for (String key : new String[]{"SPACE", "ENTER", "ESCAPE"}) {
            getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), "skip");
        }
        getActionMap().put("skip", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { skip(); }
        });

        anim = new Timer(33, e -> tick());
        anim.start();
    }

    // ------------------------------------------------------------------
    // 상태 / 애니메이션
    // ------------------------------------------------------------------

    /** 화면에 다시 들어올 때 호출: 이어하기 버튼 갱신 + 초기 상태로 */
    public void refresh() {
        boolean has = SaveManager.hasSave();
        continueBtn.setEnabled(has);
        continueBtn.setText(has ? "이어하기" : "이어하기 (없음)");
        setState(State.IDLE);
        menuBox.setVisible(true);
    }

    private void setState(State s) {
        state = s;
        stateStart = System.currentTimeMillis();
    }

    private double elapsed() {
        return (System.currentTimeMillis() - stateStart) / 1000.0;
    }

    private void startSequence(boolean story, Runnable onDone) {
        if (state != State.IDLE) return;
        showStory = story;
        afterOpen = onDone;
        menuBox.setVisible(false);
        SoundManager.getInstance().play(SoundManager.Effect.ITEM);
        setState(State.OPENING);
    }

    private void skip() {
        if (state == State.STORY || (state == State.OPENING && elapsed() > 0.5)) finish();
    }

    private void finish() {
        if (state == State.IDLE) return;
        Runnable r = afterOpen;
        afterOpen = null;
        setState(State.IDLE);
        menuBox.setVisible(true);
        if (r != null) r.run();
    }

    private void tick() {
        if (!isShowing()) return;
        frame++;
        for (int i = 0; i < flicker.length; i++) {
            double target = 0.82 + rnd.nextDouble() * 0.36;
            flicker[i] += (target - flicker[i]) * 0.35;
        }
        // 횃불에서 피어오르는 불씨
        if (rnd.nextDouble() < 0.5) {
            int side = rnd.nextInt(2);
            embers.add(new Ember(torchX(side) + rnd.nextInt(16) - 8, 300, rnd));
        }
        embers.removeIf(Ember::update);

        if (state == State.OPENING && elapsed() > 2.7) {
            if (showStory) {
                setState(State.STORY);
            } else {
                finish();
            }
        } else if (state == State.STORY && elapsed() > STORY.length * 1.7 + 1.6) {
            finish();
        }
        repaint();
    }

    private int torchX(int side) {
        return side == 0 ? DOOR_CX - 245 : DOOR_CX + 245;
    }

    // ------------------------------------------------------------------
    // 그리기
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (state == State.STORY) {
            paintStory(g2);
            g2.dispose();
            return;
        }

        if (wallImg == null) wallImg = renderWall();
        g2.drawImage(wallImg, 0, 0, null);

        // 횃불 빛
        for (int i = 0; i < 2; i++) {
            paintGlow(g2, torchX(i), 290, (float) (330 * flicker[i]), 95);
        }

        paintHourglass(g2, 175, 260);
        paintDoor(g2);
        for (int i = 0; i < 2; i++) paintTorch(g2, torchX(i), 300, flicker[i]);

        for (Ember e : embers) e.paint(g2);

        paintTitle(g2);

        g2.setFont(Theme.font(Font.PLAIN, 12));
        g2.setColor(new Color(150, 130, 100, 160));
        g2.drawString("객체지향 프로그래밍 팀 프로젝트  ·  E조", 22, H - 18);

        // 문 열림 → 하얗게 번지는 빛
        if (state == State.OPENING) {
            double t = elapsed();
            if (t > 1.6) {
                int a = (int) Math.min(255, (t - 1.6) / 1.0 * 255);
                g2.setColor(new Color(255, 240, 205, a));
                g2.fillRect(0, 0, W, H);
            }
        }
        g2.dispose();
    }

    private BufferedImage renderWall() {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random r = new Random(7);

        g.setColor(new Color(20, 17, 15));
        g.fillRect(0, 0, W, H);

        // 돌벽 벽돌
        int floorY = 640;
        int bh = 40, bw = 96;
        for (int row = 0; row * bh < floorY; row++) {
            int off = (row % 2) * (bw / 2);
            for (int x = -bw; x < W + bw; x += bw) {
                int bx = x + off + r.nextInt(5) - 2;
                int by = row * bh;
                int base = 48 + r.nextInt(18);
                g.setColor(new Color(base + 6, base, base - 6));
                g.fillRoundRect(bx + 2, by + 2, bw - 4, bh - 4, 6, 6);
                g.setColor(new Color(255, 240, 220, 14));
                g.fillRect(bx + 3, by + 3, bw - 6, 4);
                g.setColor(new Color(0, 0, 0, 50));
                g.fillRect(bx + 3, by + bh - 8, bw - 6, 5);
                for (int k = 0; k < 6; k++) {   // 얼룩 / 금
                    g.setColor(new Color(0, 0, 0, 20 + r.nextInt(25)));
                    g.fillOval(bx + r.nextInt(bw - 10), by + r.nextInt(bh - 10), 3 + r.nextInt(9), 2 + r.nextInt(5));
                }
            }
        }

        // 돌바닥
        g.setPaint(new GradientPaint(0, floorY, new Color(42, 36, 32), 0, H, new Color(14, 12, 10)));
        g.fillRect(0, floorY, W, H - floorY);
        g.setColor(new Color(0, 0, 0, 90));
        g.fillRect(0, floorY, W, 6);
        g.setColor(new Color(0, 0, 0, 70));
        g.setStroke(new BasicStroke(2f));
        for (int i = -12; i <= 12; i++) {     // 원근감 있는 바닥 줄눈
            g.drawLine(DOOR_CX + i * 60, floorY, DOOR_CX + i * 190, H);
        }
        for (int y = floorY + 18, step = 18; y < H; step += 8, y += step) {
            g.drawLine(0, y, W, y);
        }

        // 바닥에 흩어진 쇠사슬
        g.setStroke(new BasicStroke(3f));
        g.setColor(new Color(70, 66, 62));
        for (int i = 0; i < 9; i++) {
            g.drawOval(330 + i * 17, 690 + (int) (Math.sin(i) * 6), 20, 12);
        }

        // 가장자리 어둡게 (비네팅)
        g.setPaint(new RadialGradientPaint(new Point2D.Float(DOOR_CX, 380), 900,
            new float[]{0f, 0.55f, 1f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120), new Color(0, 0, 0, 235)}));
        g.fillRect(0, 0, W, H);
        g.dispose();
        return img;
    }

    private void paintGlow(Graphics2D g2, int x, int y, float radius, int alpha) {
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(x, y), radius,
            new float[]{0f, 1f},
            new Color[]{new Color(255, 150, 60, alpha), new Color(255, 120, 40, 0)}));
        g2.fillOval((int) (x - radius), (int) (y - radius), (int) (radius * 2), (int) (radius * 2));
    }

    private Shape doorShape(int x, int y, int w, int h) {
        Area a = new Area(new Rectangle2D.Double(x, y + w / 2.0, w, h - w / 2.0));
        a.add(new Area(new Ellipse2D.Double(x, y, w, w)));
        return a;
    }

    private BufferedImage renderDoorTexture() {
        BufferedImage img = new BufferedImage(DOOR_W, DOOR_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Random r = new Random(3);
        g.setClip(doorShape(0, 0, DOOR_W, DOOR_H));

        // 나무 판자
        int plank = 36;
        for (int x = 0; x < DOOR_W; x += plank) {
            int base = 78 + r.nextInt(16);
            g.setColor(new Color(base + 12, base - 22, base - 52));
            g.fillRect(x, 0, plank, DOOR_H);
            g.setColor(new Color(0, 0, 0, 40));
            for (int k = 0; k < 7; k++) {            // 나뭇결
                int gx = x + 4 + r.nextInt(plank - 8);
                g.drawLine(gx, 0, gx + r.nextInt(5) - 2, DOOR_H);
            }
            g.setColor(new Color(25, 14, 6));
            g.fillRect(x, 0, 2, DOOR_H);
        }
        // 쇠 띠 + 리벳
        for (int by : new int[]{125, 300}) {
            g.setPaint(new GradientPaint(0, by, new Color(70, 66, 62), 0, by + 20, new Color(30, 28, 26)));
            g.fillRect(0, by, DOOR_W, 20);
            g.setColor(new Color(120, 112, 100));
            for (int x = 12; x < DOOR_W; x += 34) g.fillOval(x, by + 6, 8, 8);
        }
        // 가운데 갈라진 틈
        g.setColor(new Color(15, 8, 4));
        g.fillRect(DOOR_W / 2 - 2, 0, 4, DOOR_H);
        // 위쪽 그림자
        g.setPaint(new GradientPaint(0, 0, new Color(0, 0, 0, 150), 0, 120, new Color(0, 0, 0, 0)));
        g.fillRect(0, 0, DOOR_W, 120);
        g.dispose();
        return img;
    }

    private void paintDoor(Graphics2D g2) {
        int x = DOOR_CX - DOOR_W / 2, y = DOOR_TOP;
        if (doorImg == null) doorImg = renderDoorTexture();

        // 돌 아치 테두리
        Shape outer = doorShape(x - 30, y - 30, DOOR_W + 60, DOOR_H + 30);
        g2.setColor(new Color(72, 65, 58));
        g2.fill(outer);
        g2.setColor(new Color(35, 30, 26));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(outer);
        for (int i = 0; i < 9; i++) {      // 아치 돌 블록 이음새
            double ang = Math.PI + i * Math.PI / 8;
            double cx = DOOR_CX, cy = y + DOOR_W / 2.0;
            g2.drawLine((int) (cx + Math.cos(ang) * DOOR_W / 2), (int) (cy + Math.sin(ang) * DOOR_W / 2),
                (int) (cx + Math.cos(ang) * (DOOR_W / 2 + 30)), (int) (cy + Math.sin(ang) * (DOOR_W / 2 + 30)));
        }
        // 아치 꼭대기 쐐기돌의 문양
        g2.setColor(new Color(150, 120, 60));
        g2.setFont(Theme.font(Font.BOLD, 20));
        g2.drawString("Ⅲ", DOOR_CX - 9, y - 6);

        Shape door = doorShape(x, y, DOOR_W, DOOR_H);
        double t = state == State.OPENING ? elapsed() : 0;
        double open = clamp((t - 0.6) / 1.2);
        open = open * open * (3 - 2 * open);   // ease in-out

        Shape oldClip = g2.getClip();
        g2.clip(door);
        if (open > 0) {
            // 문 너머의 빛
            g2.setPaint(new RadialGradientPaint(new Point2D.Float(DOOR_CX, y + DOOR_H * 0.55f), DOOR_H * 0.8f,
                new float[]{0f, 1f}, new Color[]{new Color(255, 250, 225), new Color(255, 170, 70)}));
            g2.fill(door);
        }
        int half = DOOR_W / 2;
        int hw = (int) Math.max(1, half * (1 - open * 0.88));
        g2.drawImage(doorImg, x, y, x + hw, y + DOOR_H, 0, 0, half, DOOR_H, null);
        g2.drawImage(doorImg, x + DOOR_W - hw, y, x + DOOR_W, y + DOOR_H, half, 0, DOOR_W, DOOR_H, null);
        if (open > 0) {
            g2.setColor(new Color(0, 0, 0, (int) (open * 140)));
            g2.fillRect(x, y, hw, DOOR_H);
            g2.fillRect(x + DOOR_W - hw, y, hw, DOOR_H);
        }
        g2.setClip(oldClip);

        // 봉인 쇠사슬 + 자물쇠 (열릴 때 사라짐)
        float sealAlpha = (float) (1 - clamp((t - 0.25) / 0.4));
        if (sealAlpha > 0) {
            Composite oc = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, sealAlpha));
            paintChain(g2, x + 10, y + 120, x + DOOR_W - 10, y + 380);
            paintChain(g2, x + DOOR_W - 10, y + 120, x + 10, y + 380);
            double lift = clamp(t / 0.25) * 14;
            paintPadlock(g2, DOOR_CX, y + 250, lift);
            g2.setComposite(oc);
        }
    }

    private void paintChain(Graphics2D g2, int x1, int y1, int x2, int y2) {
        double len = Point2D.distance(x1, y1, x2, y2);
        double ang = Math.atan2(y2 - y1, x2 - x1);
        int links = (int) (len / 22);
        AffineTransform old = g2.getTransform();
        g2.translate(x1, y1);
        g2.rotate(ang);
        for (int i = 0; i < links; i++) {
            boolean flat = i % 2 == 0;
            int lx = i * 22;
            g2.setColor(new Color(25, 23, 22));
            g2.setStroke(new BasicStroke(6f));
            if (flat) g2.drawRoundRect(lx, -7, 30, 14, 12, 12); else g2.drawLine(lx + 2, 0, lx + 28, 0);
            g2.setColor(new Color(125, 118, 108));
            g2.setStroke(new BasicStroke(3f));
            if (flat) g2.drawRoundRect(lx, -7, 30, 14, 12, 12); else g2.drawLine(lx + 2, 0, lx + 28, 0);
        }
        g2.setTransform(old);
    }

    private void paintPadlock(Graphics2D g2, int cx, int cy, double lift) {
        // 고리
        g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(40, 36, 32));
        g2.draw(new Arc2D.Double(cx - 24, cy - 52 - lift, 48, 56, 0, 180, Arc2D.OPEN));
        g2.drawLine(cx - 24, (int) (cy - 24 - lift), cx - 24, (int) (cy - 4 - lift));
        g2.drawLine(cx + 24, (int) (cy - 24 - lift), cx + 24, cy - 4);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(150, 145, 135));
        g2.draw(new Arc2D.Double(cx - 24, cy - 52 - lift, 48, 56, 0, 180, Arc2D.OPEN));

        // 몸통 (황동)
        RoundRectangle2D body = new RoundRectangle2D.Double(cx - 38, cy - 8, 76, 66, 14, 14);
        g2.setPaint(new GradientPaint(cx - 38, cy, new Color(205, 160, 70), cx + 38, cy + 60, new Color(110, 75, 25)));
        g2.fill(body);
        g2.setColor(new Color(60, 40, 15));
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(body);

        // 열쇠구멍 + 맥박처럼 빛나는 기운
        double pulse = 0.5 + 0.5 * Math.sin(frame * 0.12);
        paintGlow(g2, cx, cy + 22, (float) (26 + pulse * 10), (int) (120 + pulse * 90));
        g2.setColor(new Color(15, 8, 4));
        g2.fillOval(cx - 8, cy + 10, 16, 16);
        g2.fillPolygon(new int[]{cx - 6, cx + 6, cx + 3, cx - 3}, new int[]{cy + 20, cy + 20, cy + 42, cy + 42}, 4);
    }

    private void paintTorch(Graphics2D g2, int x, int y, double f) {
        // 벽 거치대
        g2.setColor(new Color(30, 28, 26));
        g2.fillRect(x - 18, y + 70, 36, 10);
        g2.fillRect(x - 4, y + 60, 8, 40);
        // 손잡이
        g2.setPaint(new GradientPaint(x - 8, y, new Color(95, 60, 30), x + 8, y, new Color(50, 30, 14)));
        g2.fill(new Polygon(new int[]{x - 11, x + 11, x + 6, x - 6}, new int[]{y + 4, y + 4, y + 80, y + 80}, 4));
        g2.setColor(new Color(40, 36, 32));
        g2.fillRect(x - 13, y, 26, 10);
        // 불꽃 (바깥 → 안쪽)
        double h = 62 * f;
        Color[] cols = {new Color(200, 60, 20, 200), new Color(255, 140, 30, 230), new Color(255, 225, 120)};
        double[] scale = {1.0, 0.72, 0.42};
        for (int i = 0; i < 3; i++) {
            double fw = 26 * scale[i], fh = h * scale[i];
            double sway = Math.sin(frame * 0.25 + x) * 3 * (1 - scale[i] + 0.3);
            Path2D p = new Path2D.Double();
            p.moveTo(x - fw / 2, y + 2);
            p.curveTo(x - fw / 2 - 4, y - fh * 0.4, x + sway - 2, y - fh * 0.7, x + sway, y - fh);
            p.curveTo(x + sway + 2, y - fh * 0.7, x + fw / 2 + 4, y - fh * 0.4, x + fw / 2, y + 2);
            p.closePath();
            g2.setColor(cols[i]);
            g2.fill(p);
        }
    }

    private void paintHourglass(Graphics2D g2, int cx, int top) {
        int hgW = 120, hgH = 230;
        int midY = top + hgH / 2;
        double cycle = 30.0;                                    // 30초마다 모래가 다 떨어짐
        double p = (System.currentTimeMillis() / 1000.0 % cycle) / cycle;

        paintGlow(g2, cx, midY, 150, 35);

        // 유리 모양
        Path2D glass = new Path2D.Double();
        glass.moveTo(cx - hgW / 2.0, top + 14);
        glass.curveTo(cx - hgW / 2.0, midY - 30, cx - 8, midY - 14, cx - 6, midY);
        glass.curveTo(cx - 8, midY + 14, cx - hgW / 2.0, midY + 30, cx - hgW / 2.0, top + hgH - 14);
        glass.lineTo(cx + hgW / 2.0, top + hgH - 14);
        glass.curveTo(cx + hgW / 2.0, midY + 30, cx + 8, midY + 14, cx + 6, midY);
        glass.curveTo(cx + 8, midY - 14, cx + hgW / 2.0, midY - 30, cx + hgW / 2.0, top + 14);
        glass.closePath();
        g2.setColor(new Color(180, 200, 210, 40));
        g2.fill(glass);

        Shape oldClip = g2.getClip();
        g2.clip(glass);
        Color sand = new Color(225, 180, 95);
        // 위쪽 모래 (줄어듦)
        int topSandTop = (int) (top + 30 + (midY - top - 34) * p);
        g2.setColor(sand);
        g2.fillRect(cx - hgW / 2, topSandTop, hgW, midY - topSandTop);
        // 아래쪽 모래 (쌓임, 산 모양)
        int pileH = (int) ((hgH / 2.0 - 30) * p);
        int bottom = top + hgH - 14;
        g2.fillPolygon(new int[]{cx - hgW / 2, cx, cx + hgW / 2, cx + hgW / 2, cx - hgW / 2},
            new int[]{bottom - pileH, bottom - pileH - 18, bottom - pileH, bottom, bottom}, 5);
        // 떨어지는 모래 줄기
        if (p < 0.98) {
            g2.setColor(new Color(240, 200, 120));
            g2.fillRect(cx - 1, midY, 3, bottom - pileH - 18 - midY);
        }
        g2.setClip(oldClip);

        g2.setColor(new Color(220, 235, 240, 120));
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(glass);
        // 위/아래 나무 받침 + 기둥
        g2.setPaint(new GradientPaint(0, top, new Color(110, 70, 35), 0, top + 16, new Color(55, 32, 15)));
        g2.fillRoundRect(cx - hgW / 2 - 16, top, hgW + 32, 16, 6, 6);
        g2.fillRoundRect(cx - hgW / 2 - 16, top + hgH - 16, hgW + 32, 16, 6, 6);
        g2.setColor(new Color(70, 44, 22));
        g2.fillRect(cx - hgW / 2 - 10, top + 14, 7, hgH - 28);
        g2.fillRect(cx + hgW / 2 + 3, top + 14, 7, hgH - 28);

        g2.setFont(Theme.font(Font.BOLD, 15));
        g2.setColor(new Color(220, 190, 130, 200));
        String s = "시간은 기다려주지 않는다";
        g2.drawString(s, cx - g2.getFontMetrics().stringWidth(s) / 2, top + hgH + 34);
    }

    private void paintTitle(Graphics2D g2) {
        Font f = Theme.font(Font.BOLD, 72);
        GlyphVector gv = f.createGlyphVector(g2.getFontRenderContext(), "ESCAPE ROOM");
        Rectangle2D b = gv.getVisualBounds();
        int tx = (int) (DOOR_CX - b.getWidth() / 2 - b.getX());
        int ty = 112;
        Shape text = gv.getOutline(tx, ty);

        // 은은한 빛 번짐
        double pulse = 0.5 + 0.5 * Math.sin(frame * 0.06);
        for (int i = 5; i >= 1; i--) {
            g2.setColor(new Color(255, 170, 60, (int) (10 + pulse * 8)));
            g2.setStroke(new BasicStroke(i * 4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(text);
        }
        g2.setPaint(new GradientPaint(0, ty - 60, new Color(255, 240, 180), 0, ty, new Color(190, 130, 50)));
        g2.fill(text);
        g2.setColor(new Color(50, 30, 10));
        g2.setStroke(new BasicStroke(1.5f));
        g2.draw(text);

        g2.setFont(Theme.font(Font.BOLD, 20));
        String sub = "— 봉인된 성에서 제한 시간 안에 탈출하라 —";
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(sub, DOOR_CX - g2.getFontMetrics().stringWidth(sub) / 2 + 2, 152);
        g2.setColor(new Color(235, 215, 175));
        g2.drawString(sub, DOOR_CX - g2.getFontMetrics().stringWidth(sub) / 2, 150);
    }

    private void paintMenuPlate(Graphics2D g2, int w, int h) {
        g2 = (Graphics2D) g2.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RoundRectangle2D plate = new RoundRectangle2D.Double(4, 4, w - 8, h - 8, 22, 22);
        g2.setPaint(new GradientPaint(0, 0, new Color(44, 32, 22, 235), 0, h, new Color(20, 15, 11, 240)));
        g2.fill(plate);
        g2.setColor(Theme.GOLD);
        g2.setStroke(new BasicStroke(2.5f));
        g2.draw(plate);
        g2.setColor(new Color(205, 165, 85, 90));
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(12, 12, w - 24, h - 24, 16, 16));
        // 네 모서리 리벳
        g2.setColor(new Color(160, 130, 70));
        for (int[] c : new int[][]{{18, 18}, {w - 26, 18}, {18, h - 26}, {w - 26, h - 26}}) {
            g2.fillOval(c[0], c[1], 8, 8);
        }
        g2.dispose();
    }

    private void paintStory(Graphics2D g2) {
        double t = elapsed();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, W, H);

        int startY = H / 2 - STORY.length * 26;
        for (int i = 0; i < STORY.length; i++) {
            double a = clamp((t - 0.6 - i * 1.7) / 0.8);
            if (a <= 0) continue;
            boolean quote = STORY[i].startsWith("\"");
            g2.setFont(Theme.font(quote ? Font.BOLD : Font.PLAIN, quote ? 24 : 22));
            g2.setColor(quote ? new Color(255, 215, 130, (int) (a * 255)) : new Color(225, 210, 185, (int) (a * 255)));
            int sw = g2.getFontMetrics().stringWidth(STORY[i]);
            g2.drawString(STORY[i], (W - sw) / 2, startY + i * 52 + (int) ((1 - a) * 10));
        }

        // 처음엔 하얀 빛에서 어둠으로
        if (t < 0.8) {
            g2.setColor(new Color(255, 240, 205, (int) ((1 - t / 0.8) * 255)));
            g2.fillRect(0, 0, W, H);
        }

        g2.setFont(Theme.font(Font.PLAIN, 14));
        g2.setColor(new Color(160, 140, 110, (int) (140 + 100 * Math.sin(frame * 0.1))));
        String skip = "클릭 또는 SPACE 키로 건너뛰기";
        g2.drawString(skip, W - g2.getFontMetrics().stringWidth(skip) - 30, H - 26);
    }

    private static double clamp(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private String getPlayerName() {
        String n = nameField.getText().trim();
        return n.isEmpty() ? "플레이어" : n;
    }

    private Difficulty getDifficulty() {
        Difficulty[] ds = Difficulty.values();
        for (int i = 0; i < ds.length; i++) if (diffButtons[i].isSelected()) return ds[i];
        return Difficulty.NORMAL;
    }

    /** 횃불에서 떠오르는 불씨 하나 */
    private static class Ember {
        double x, y, vx, vy, life;
        final double size;

        Ember(double x, double y, Random r) {
            this.x = x;
            this.y = y;
            vx = (r.nextDouble() - 0.5) * 0.8;
            vy = -1.2 - r.nextDouble() * 1.5;
            life = 1.0;
            size = 2 + r.nextDouble() * 2.5;
        }

        /** 수명이 다하면 true */
        boolean update() {
            x += vx + Math.sin(y * 0.05) * 0.4;
            y += vy;
            life -= 0.012;
            return life <= 0;
        }

        void paint(Graphics2D g2) {
            int a = (int) (Math.max(0, life) * 220);
            g2.setColor(new Color(255, 170 + (int) (life * 60), 70, a));
            g2.fill(new Ellipse2D.Double(x, y, size, size));
        }
    }
}
