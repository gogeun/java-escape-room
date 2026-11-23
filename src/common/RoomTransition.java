package common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

/**
 * 방을 옮길 때 방 화면 위에 그려지는 연출 레이어 (1번 방의 승강기 도착 연출과 같은 형식).
 *
 *  - fade()    : 이전 방이 어두워졌다가 새 방이 밝아진다
 *  - passage() : 2번 방 → 3번 방. 칠흑 같은 어둠 속에서 문틈으로 붉은 빛이 새어 나오고,
 *                문이 열리면 그 안으로 들어가 어두운 돌계단을 따라 내려간 뒤 새 방이 밝아진다
 *  - caption() : 화면 아래에 도착한 장소 이름이 떠올랐다 사라진다 (클릭은 방으로 통과)
 *
 * 연출이 진행 중일 때(caption 제외)는 클릭을 막는다.
 */
public class RoomTransition extends JComponent {
    private enum Mode { NONE, FADE, PASSAGE, CAPTION }

    private static final double FADE_OUT = 0.45, FADE_IN = 0.55;
    // 지하 계단 연출 시간표 (초)
    private static final double P_DARK = 0.6;       // 이전 방이 어둠에 잠김
    private static final double P_SLIT = 1.0;       // 어둠 속 문틈의 빛
    private static final double P_OPEN = 2.4;       // 문이 열림 (P_SLIT ~ P_OPEN)
    private static final double P_ENTER = 3.1;      // 문 안으로 들어감
    private static final double P_STAIRS = 5.4;     // 계단을 내려감 (P_ENTER ~ P_STAIRS)
    private static final double P_TITLE = 5.4;      // 계단 끝 (여기서부터 새 방이 밝아짐)
    private static final double P_END = 6.2;        // 새 방이 드러남 → 아래 자막으로 장소 이름
    private static final double C_IN = 0.5, C_HOLD = 2.6, C_OUT = 0.7;

    private Mode mode = Mode.NONE;
    private long start;
    private BufferedImage snapshot;
    private RoomTheme from = RoomTheme.DEFAULT, to = RoomTheme.DEFAULT;
    private String place = "", detail = "";
    private Runnable onEnd;
    private final Timer timer = new Timer(1000 / 30, e -> tick());

    public RoomTransition() {
        setVisible(false);
        addMouseListener(new java.awt.event.MouseAdapter() {});   // 연출 중 클릭 막기
    }

    /** 연출 중이라 조작을 막아야 하는지 */
    public boolean isBlocking() {
        return mode == Mode.FADE || mode == Mode.PASSAGE;
    }

    @Override public boolean contains(int x, int y) {
        return isBlocking() && super.contains(x, y);
    }

    public void fade(BufferedImage oldRoom, String place, String detail, RoomTheme theme) {
        begin(Mode.FADE, oldRoom, theme, theme, place, detail, null);
    }

    public void passage(BufferedImage oldRoom, RoomTheme from, RoomTheme to, String place, String detail, Runnable onEnd) {
        begin(Mode.PASSAGE, oldRoom, from, to, place, detail, onEnd);
    }

    public void caption(String place, String detail, RoomTheme theme) {
        begin(Mode.CAPTION, null, theme, theme, place, detail, null);
    }

    private void begin(Mode m, BufferedImage snap, RoomTheme f, RoomTheme t, String p, String d, Runnable end) {
        mode = m;
        snapshot = snap;
        from = f;
        to = t;
        place = p;
        detail = d;
        onEnd = end;
        start = System.nanoTime();
        setVisible(true);
        timer.restart();
        repaint();
    }

    private double elapsed() {
        return (System.nanoTime() - start) / 1e9;
    }

    private void tick() {
        double t = elapsed();
        if (mode == Mode.FADE && t >= (snapshot == null ? FADE_IN : FADE_OUT + FADE_IN)) {
            begin(Mode.CAPTION, null, to, to, place, detail, null);
        } else if (mode == Mode.PASSAGE && t >= P_END) {
            Runnable end = onEnd;
            begin(Mode.CAPTION, null, to, to, place, detail, null);
            if (end != null) end.run();
        } else if (mode == Mode.CAPTION && t >= C_IN + C_HOLD + C_OUT) {
            stop();
        }
        repaint();
    }

    public void stop() {
        timer.stop();
        mode = Mode.NONE;
        snapshot = null;
        setVisible(false);
    }

    // ------------------------------------------------------------------
    // 그리기
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        RoomTheme.smooth(g2);
        double t = elapsed();
        switch (mode) {
            case FADE -> paintFade(g2, t);
            case PASSAGE -> paintPassageWithReveal(g2, t);
            case CAPTION -> paintCaption(g2, t);
            default -> { }
        }
        g2.dispose();
    }

    private void paintFade(Graphics2D g2, double t) {
        int w = getWidth(), h = getHeight();
        double out = snapshot == null ? 0 : FADE_OUT;
        if (t < out) {
            g2.drawImage(snapshot, 0, 0, w, h, null);
            g2.setColor(new Color(0, 0, 0, (int) (255 * ease(t / out))));
        } else {
            g2.setColor(new Color(0, 0, 0, (int) (255 * (1 - ease((t - out) / FADE_IN)))));
        }
        g2.fillRect(0, 0, w, h);
    }

    /** 마지막 구간에는 연출 화면을 반투명하게 그려서 아래의 새 방이 서서히 보이게 한다 */
    private void paintPassageWithReveal(Graphics2D g2, double t) {
        if (t <= P_TITLE) {
            paintPassage(g2, t);
            return;
        }
        int w = Math.max(1, getWidth()), h = Math.max(1, getHeight());
        BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D b = buf.createGraphics();
        RoomTheme.smooth(b);
        paintPassage(b, t);
        b.dispose();
        double k = (t - P_TITLE) / (P_END - P_TITLE);
        g2.setComposite(AlphaComposite.SrcOver.derive((float) (1 - ease(k))));
        g2.drawImage(buf, 0, 0, null);
    }

    /** 2번 방 → 3번 방: 어둠 → 문틈의 빛 → 문이 열림 → 안으로 → 어두운 계단을 내려감 → 붉은 빛 */
    private void paintPassage(Graphics2D g2, double t) {
        int w = getWidth(), h = getHeight();

        // ① 이전 방이 어둠에 잠긴다
        if (t < P_DARK) {
            if (snapshot != null) g2.drawImage(snapshot, 0, 0, w, h, null);
            g2.setColor(new Color(0, 0, 0, (int) (255 * ease(t / P_DARK))));
            g2.fillRect(0, 0, w, h);
            return;
        }
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, w, h);

        // ② ~ ④ 문 장면 (들어가는 동안 계단 장면과 겹쳐짐)
        if (t < P_ENTER + 0.35) {
            double open = ease((t - P_SLIT) / (P_OPEN - P_SLIT));
            double push = ease((t - P_OPEN) / (P_ENTER - P_OPEN));
            double appear = ease((t - P_DARK) / (P_SLIT - P_DARK + 0.3));
            Graphics2D d = (Graphics2D) g2.create();
            double s = 1 + push * 3.2;                                   // 문 쪽으로 다가감
            d.translate(w / 2.0, h * 0.55);
            d.scale(s, s);
            d.translate(-w / 2.0, -h * 0.55);
            drawDoorScene(d, w, h, t, open, appear);
            d.dispose();
        }

        // ⑤ 계단을 내려간다
        if (t > P_ENTER - 0.25) {
            double fadeIn = ease((t - (P_ENTER - 0.25)) / 0.6);
            double walk = Math.max(0, t - (P_ENTER - 0.25));
            double p = Math.min(1, walk / (P_STAIRS - P_ENTER + 0.25));
            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.SrcOver.derive((float) fadeIn));
            drawStairs(g2, w, h, walk, p);
            g2.setComposite(old);
        }

        // ⑥ 계단 끝에서 어둠에 잠긴 뒤, 바로 3번 방 화면이 밝아진다 (장소 이름은 방 화면 아래 자막으로)
        if (t > P_STAIRS - 0.5) {
            double k = Math.min(1, (t - (P_STAIRS - 0.5)) / 0.5);
            g2.setColor(new Color(0, 0, 0, (int) (255 * ease(k))));
            g2.fillRect(0, 0, w, h);
        }
    }

    // ---------------- 문 장면 ----------------

    private void drawDoorScene(Graphics2D g2, int w, int h, double t, double open, double appear) {
        double cx = w / 2.0, top = h * 0.55 - 200, dw = 230, dh = 380;
        double left = cx - dw / 2, bottom = top + dh;
        Color light = mix(new Color(255, 150, 80), to.heart, 0.55);
        double flick = 0.9 + 0.1 * Math.sin(t * 13) * Math.sin(t * 7.3);

        // 문 너머의 빛 (열릴수록 커짐)
        double glowR = 140 + 520 * open;
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, bottom - 60), (float) glowR,
            new float[]{0, 1}, new Color[]{withAlpha(light, (int) ((40 + 150 * open) * appear * flick)), new Color(0, 0, 0, 0)}));
        g2.fillRect(0, 0, w, h);

        // 돌 문틀 (빛에 살짝 드러남)
        Path2D frame = archShape(left - 34, top - 30, dw + 68, dh + 30);
        g2.setPaint(new GradientPaint(0, (float) top, withAlpha(new Color(70, 58, 50), (int) (255 * appear * (0.35 + 0.65 * open))),
            0, (float) bottom, withAlpha(new Color(30, 24, 22), (int) (255 * appear))));
        g2.fill(frame);

        // 문 안쪽: 빛 + 아래로 내려가는 첫 계단 실루엣
        Shape opening = archShape(left, top, dw, dh);
        Graphics2D in = (Graphics2D) g2.create();
        in.clip(opening);
        double inner = appear * (0.08 + 0.92 * open);                   // 닫혀 있으면 안쪽은 거의 깜깜
        in.setColor(Color.BLACK);
        in.fill(opening);
        in.setPaint(new GradientPaint(0, (float) top, withAlpha(scale(light, 0.35), (int) (255 * inner)),
            0, (float) bottom, withAlpha(light, (int) (255 * inner * flick))));
        in.fill(opening);
        in.dispose();

        // 문짝 두 개 (경첩을 축으로 바깥쪽으로 열림)
        double ang = open * Math.toRadians(82);
        java.awt.geom.Area leafClip = new java.awt.geom.Area(opening);          // 문 폭 안에서는 아치 모양으로 잘림
        leafClip.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(0, 0, left, h)));
        leafClip.add(new java.awt.geom.Area(new java.awt.geom.Rectangle2D.Double(left + dw, 0, w, h)));
        Graphics2D lg = (Graphics2D) g2.create();
        lg.clip(leafClip);
        drawLeaf(lg, left, top, dw / 2, dh, ang, true, appear, light, open);
        drawLeaf(lg, cx + dw / 2, top, dw / 2, dh, ang, false, appear, light, open);
        lg.dispose();

        // 닫혀 있을 때 문틈으로 새는 빛
        if (open < 0.25) {
            double slit = (1 - open / 0.25) * appear * flick;
            g2.setColor(withAlpha(light, (int) (230 * slit)));
            g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine((int) cx, (int) top + 30, (int) cx, (int) bottom);          // 가운데 틈
            g2.draw(new java.awt.geom.Line2D.Double(left + 6, bottom + 1, left + dw - 6, bottom + 1));   // 문 아래 틈
            g2.setPaint(new GradientPaint(0, (float) bottom, withAlpha(light, (int) (110 * slit)), 0, (float) bottom + 70, new Color(0, 0, 0, 0)));
            g2.fill(new java.awt.geom.Rectangle2D.Double(left - 40, bottom, dw + 80, 70));   // 바닥에 번지는 빛
            g2.setStroke(new BasicStroke(1));
        }
    }

    /** 문짝 하나. hingeLeft=true 면 왼쪽 경첩, 열리면 화면 쪽으로 돌아오며 좁아 보인다 */
    private void drawLeaf(Graphics2D g2, double x, double top, double lw, double lh, double ang,
                          boolean hingeLeft, double appear, Color light, double open) {
        double vis = lw * Math.cos(ang);                 // 정면에서 보이는 폭
        double grow = lw * Math.sin(ang) * 0.12;         // 다가오는 쪽 모서리가 커 보임 (원근)
        double hx = hingeLeft ? x : x;                   // 경첩 x
        double ex = hingeLeft ? hx + vis : hx - vis;     // 바깥 모서리 x
        Path2D leaf = new Path2D.Double();
        leaf.moveTo(hx, top);
        leaf.lineTo(ex, top - grow);
        leaf.lineTo(ex, top + lh + grow);
        leaf.lineTo(hx, top + lh);
        leaf.closePath();
        int a = (int) (255 * appear);
        Color wood = scale(new Color(74, 44, 28), 0.55 + 0.25 * (1 - open));
        g2.setPaint(new GradientPaint((float) hx, 0, withAlpha(scale(wood, 0.6), a), (float) ex, 0, withAlpha(wood, a)));
        g2.fill(leaf);
        g2.setColor(withAlpha(new Color(20, 12, 8), a));                   // 널빤지 줄
        for (int i = 1; i < 4; i++) {
            double px = hx + (ex - hx) * i / 4.0;
            g2.draw(new java.awt.geom.Line2D.Double(px, top - grow * i / 4.0, px, top + lh + grow * i / 4.0));
        }
        g2.setColor(withAlpha(new Color(40, 38, 40), a));                  // 쇠 띠
        for (double fy : new double[]{0.25, 0.75}) {
            double y1 = top + lh * fy;
            g2.fill(new java.awt.geom.Rectangle2D.Double(Math.min(hx, ex), y1 - 5, Math.abs(ex - hx), 10));
        }
        if (open > 0.05) {                                                // 문 가장자리에 비치는 빛
            g2.setColor(withAlpha(light, (int) (160 * open * appear)));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new java.awt.geom.Line2D.Double(ex, top - grow, ex, top + lh + grow));
            g2.setStroke(new BasicStroke(1));
        }
    }

    /** 바닥이 평평하고 위가 둥근 아치 모양 */
    private static Path2D archShape(double x, double y, double w, double h) {
        Path2D p = new Path2D.Double();
        double r = w / 2;
        p.moveTo(x, y + h);
        p.lineTo(x, y + r);
        p.quadTo(x, y, x + r, y);
        p.quadTo(x + w, y, x + w, y + r);
        p.lineTo(x + w, y + h);
        p.closePath();
        return p;
    }

    // ---------------- 계단 장면 ----------------

    /**
     * 어두운 돌계단을 내려가는 1인칭 시점.
     * 계단 디딤판(밝은 면)과 그 사이의 어둠이 아래쪽 소실점으로 모이고,
     * 한 칸씩 내려갈 때마다 시점이 살짝 흔들린다. 벽에는 촛불, 계단 끝에는 붉은 빛.
     */
    private void drawStairs(Graphics2D g2, int w, int h, double walk, double p) {
        double steps = walk * 2.4;                       // 초당 2.4칸
        double frac = steps % 1.0;
        double bob = Math.sin(steps * Math.PI * 2) * 5 + 4;
        double vh = h * 0.42 + bob;                      // 화면 중심 (시선은 계단 방향으로 기울어 있음)
        double cx = w / 2.0;
        final double f = 520, fx = 640, R = 0.24, RUN = 0.28, H = 1.55, Z0 = 0.55, WALL = 1.15, CEIL = 2.3;
        final double pitch = Math.atan(R / RUN) * 0.8, cos = Math.cos(pitch), sin = Math.sin(pitch);
        Color glow = mix(new Color(255, 140, 70), to.heart, 0.4 + 0.6 * p);

        g2.setColor(new Color(4, 2, 3));
        g2.fillRect(0, 0, w, h);
        // 계단 끝 (아래쪽 소실점)의 붉은 빛
        double vpStairY = vh + f * (R * cos - RUN * sin) / (RUN * cos + R * sin);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, vpStairY), (float) (160 + 260 * p),
            new float[]{0, 1}, new Color[]{withAlpha(glow, (int) (120 + 110 * p)), new Color(0, 0, 0, 0)}));
        g2.fillRect(0, 0, w, h);

        // 먼 칸부터 그림
        for (int i = 44; i >= 0; i--) {
            double u = i - frac;
            double z1 = Z0 + u * RUN, z2 = z1 + RUN;
            if (z1 < 0.12) continue;
            double drop = H + u * R;                        // 눈에서 이 디딤판까지 높이
            // 아래로 기울인 시선으로 투영 (깊이 zc, 화면 y)
            double zF = z1 * cos + drop * sin, zB = z2 * cos + drop * sin;
            double zcF = z1 * cos + (drop - CEIL) * sin, zcB = z2 * cos + (drop - CEIL) * sin;
            if (zF < 0.15 || zB < 0.15) continue;
            double yF = vh + f * (drop * cos - z1 * sin) / zF, yB = vh + f * (drop * cos - z2 * sin) / zB;
            double xF = fx * WALL / zF, xB = fx * WALL / zB;
            double cF = zcF < 0.15 ? -2000 : vh + f * ((drop - CEIL) * cos - z1 * sin) / zcF;
            double cB = zcB < 0.15 ? -2000 : vh + f * ((drop - CEIL) * cos - z2 * sin) / zcB;
            double light = Math.max(0, Math.min(1, 1.25 - z1 / 5.5));       // 멀수록 어둡게
            double lit = light * (0.55 + 0.45 * p * (z1 / 7));               // 아래쪽 붉은 빛

            double fog = Math.max(0, Math.min(1, (zF - 2.5) / 7.0));          // 멀수록 붉은 안개에 묻힘
            Color fogColor = scale(glow, 0.55 + 0.25 * p);

            // 양쪽 벽 (천장 쪽 모서리는 천장 깊이로 계산해 이어진 벽면으로)
            double xcF = zcF < 0.15 ? 4000 : Math.min(4000, fx * WALL / zcF);
            double xcB = zcB < 0.15 ? 4000 : Math.min(4000, fx * WALL / zcB);
            Color stone = mix(new Color(62, 52, 46), scale(glow, 0.45), 0.25 + 0.35 * p);
            Color wallC = mix(scale(stone, 0.45 * lit + 0.05), fogColor, fog);
            Path2D lw = quad(cx - xcF, cF, cx - xcB, cB, cx - xB, yB, cx - xF, yF);
            Path2D rw = quad(cx + xcF, cF, cx + xcB, cB, cx + xB, yB, cx + xF, yF);
            g2.setColor(wallC);
            g2.fill(lw);
            g2.fill(rw);
            g2.setColor(withAlpha(Color.BLACK, (int) (110 * (1 - fog))));    // 돌 줄눈
            g2.draw(new java.awt.geom.Line2D.Double(cx - (xF + xcF) / 2, (cF + yF) / 2, cx - (xB + xcB) / 2, (cB + yB) / 2));
            g2.draw(new java.awt.geom.Line2D.Double(cx + (xF + xcF) / 2, (cF + yF) / 2, cx + (xB + xcB) / 2, (cB + yB) / 2));
            if (i % 2 == 0) {
                g2.draw(new java.awt.geom.Line2D.Double(cx - xcF, cF, cx - xF, yF));
                g2.draw(new java.awt.geom.Line2D.Double(cx + xcF, cF, cx + xF, yF));
            }
            // 천장
            g2.setColor(mix(scale(stone, 0.22 * lit + 0.02), fogColor, fog * 0.8));
            g2.fill(quad(cx - xcF, cF, cx + xcF, cF, cx + xcB, cB, cx - xcB, cB));

            // 디딤판 (앞쪽 모서리가 밝고 안쪽이 어두움)
            Path2D tread = quad(cx - xF, yF, cx + xF, yF, cx + xB, yB, cx - xB, yB);
            Color top = mix(scale(mix(new Color(96, 84, 74), glow, 0.18 + 0.3 * p), 0.9 * lit + 0.05), fogColor, fog);
            g2.setPaint(new GradientPaint(0, (float) yB, scale(top, 0.55), 0, (float) yF, top));
            g2.fill(tread);
            g2.setColor(scale(top, 1.35));                                     // 모서리 하이라이트
            g2.setStroke(new BasicStroke((float) Math.max(1, 3 * light)));
            g2.draw(new java.awt.geom.Line2D.Double(cx - xF, yF, cx + xF, yF));
            g2.setStroke(new BasicStroke(1));
            g2.setColor(withAlpha(Color.BLACK, 170));                          // 다음 칸으로 떨어지는 그림자
            g2.draw(new java.awt.geom.Line2D.Double(cx - xB, yB, cx + xB, yB));

            // 벽의 촛불 (4칸마다)
            if (i % 4 == 1 && z1 > 0.5) {
                double flick = 0.85 + 0.15 * Math.sin(walk * 15 + i);
                double sy = (cF + yF) / 2 - (yF - cF) * 0.05;
                double size = 10 * (1.4 / z1);
                candle(g2, cx - xF + size * 0.6, sy, size * flick, light, glow);
                candle(g2, cx + xF - size * 0.6, sy, size * flick, light, glow);
            }
        }

        // 계단 끝은 붉은 안개 속으로 사라진다
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, vpStairY), (float) (90 + 70 * p),
            new float[]{0, 1}, new Color[]{withAlpha(scale(glow, 0.8), (int) (200 + 40 * p)), new Color(0, 0, 0, 0)}));
        g2.fill(new Ellipse2D.Double(cx - 170, vpStairY - 170, 340, 340));

        // 가장자리 어둡게 (손에 든 횃불 범위만 보이는 느낌)
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(cx, h * 0.62), w * 0.62f,
            new float[]{0.35f, 1f}, new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 235)}));
        g2.fillRect(0, 0, w, h);
    }

    private static Path2D quad(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        Path2D p = new Path2D.Double();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        p.lineTo(x3, y3);
        p.lineTo(x4, y4);
        p.closePath();
        return p;
    }

    private static void candle(Graphics2D g2, double x, double y, double s, double shade, Color glow) {
        if (s < 1) return;
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float) (s * 5),
            new float[]{0, 1}, new Color[]{withAlpha(glow, (int) (110 * shade)), new Color(0, 0, 0, 0)}));
        g2.fill(new Ellipse2D.Double(x - s * 5, y - s * 5, s * 10, s * 10));
        g2.setColor(withAlpha(new Color(225, 210, 190), (int) (230 * shade)));
        g2.fill(new java.awt.geom.Rectangle2D.Double(x - s * 0.25, y, s * 0.5, s * 1.4));
        g2.setColor(withAlpha(new Color(255, 215, 130), (int) (255 * shade)));
        g2.fill(new Ellipse2D.Double(x - s * 0.28, y - s * 0.9, s * 0.56, s * 0.95));
    }

    // ---------------- 도착 자막 ----------------

    /** 1번 방 승강기 도착 화면과 같은 아래쪽 자막 띠 */
    private void paintCaption(Graphics2D g2, double t) {
        int w = getWidth(), h = getHeight();
        double a = t < C_IN ? ease(t / C_IN) : t < C_IN + C_HOLD ? 1 : 1 - ease((t - C_IN - C_HOLD) / C_OUT);
        a = Math.max(0, Math.min(1, a));
        int band = 150, top = h - band - 96;                     // 가방 위쪽에 띄운다
        g2.setPaint(new GradientPaint(0, top, withAlpha(to.bgDark, 0), 0, top + band, withAlpha(to.bgDark, (int) (210 * a))));
        g2.fillRect(0, top, w, band);
        g2.setPaint(new GradientPaint(0, top + band, withAlpha(to.bgDark, (int) (210 * a)), 0, h, withAlpha(to.bgDark, 0)));
        g2.fillRect(0, top + band, w, h - top - band);

        Font serif = new Font("Batang", Font.BOLD, 30);
        g2.setFont(serif.canDisplay('성') ? serif : Theme.font(Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int rise = (int) ((1 - a) * 8);
        int y = top + band - 40 + rise;
        g2.setColor(withAlpha(to.accentLight, (int) (255 * a)));
        g2.drawString(place, (w - fm.stringWidth(place)) / 2, y);
        int lineW = Math.min(360, fm.stringWidth(place) + 80);
        g2.setColor(withAlpha(to.accent, (int) (160 * a)));
        g2.drawLine((w - lineW) / 2, y + 10, (w + lineW) / 2, y + 10);
        g2.setFont(Theme.font(Font.PLAIN, 14));
        fm = g2.getFontMetrics();
        g2.setColor(withAlpha(to.text, (int) (235 * a)));
        g2.drawString(detail, (w - fm.stringWidth(detail)) / 2, y + 32);
    }

    // ------------------------------------------------------------------

    private static double ease(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static Color mix(Color a, Color b, double k) {
        k = Math.max(0, Math.min(1, k));
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * k),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * k),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * k));
    }

    private static Color scale(Color c, double k) {
        k = Math.max(0, k);
        return new Color((int) Math.min(255, c.getRed() * k), (int) Math.min(255, c.getGreen() * k),
            (int) Math.min(255, c.getBlue() * k));
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
