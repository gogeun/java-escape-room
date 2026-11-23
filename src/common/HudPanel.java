package common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * 화면 상단 바.
 *  - 왼쪽   : 방 이름 / 진행도 / 플레이어
 *  - 가운데 : 남은 시간
 *  - 오른쪽 : 체력(하트) + 기능 버튼 (힌트 · 메모 · 저장 · 설정 · 도움말 · 일시정지)
 *
 * 1번 방의 황동 계기판 모양을 기본으로, 현재 방의 RoomTheme 색으로 그린다.
 * GameListener 를 구현하여 상태가 바뀌면 자동으로 다시 그린다.
 */
public class HudPanel extends JPanel implements GameListener {
    public static final int HEIGHT = 64;

    private String roomTitle = "";
    private int roomIndex, roomCount;
    private int hp, maxHp;
    private int remainingSec;
    private int hintsLeft;
    private int damageFlash;     // 피격 시 하트 깜빡임
    private boolean blink;       // 1분 이하 타이머 깜빡임
    private final BarButton hintButton;

    public HudPanel(MainFrame frame) {
        setLayout(null);
        setPreferredSize(new Dimension(RoomPanel.ROOM_WIDTH, HEIGHT));
        setOpaque(true);

        hintButton = new BarButton("힌트", "힌트 보기 (H)", frame::openHint);
        BarButton[] buttons = {
            hintButton,
            new BarButton("메모", "메모장 (M)", frame::openMemo),
            new BarButton("도움말", "도움말", frame::openHelp),
            new BarButton("설정", "설정 · 일시정지 (ESC)", frame::openSettings)
        };
        int bw = 78, gap = 7, bh = 42;
        int x = RoomPanel.ROOM_WIDTH - 14 - buttons.length * bw - (buttons.length - 1) * gap;
        for (BarButton b : buttons) {
            b.setBounds(x, (HEIGHT - bh) / 2, bw, bh);
            add(b);
            x += bw + gap;
        }

        Timer anim = new Timer(120, e -> {
            if (damageFlash > 0) damageFlash--;
            blink = !blink;
            if (damageFlash > 0 || remainingSec <= 60) repaint();
        });
        anim.start();
        GameManager.getInstance().addListener(this);
    }

    @Override public void onTimeChanged(int sec)        { remainingSec = sec; repaint(); }
    @Override public void onHpChanged(int hp, int max)  { this.hp = hp; this.maxHp = max; repaint(); }
    @Override public void onHintsChanged(int left)      { hintsLeft = left; repaint(); }
    @Override public void onDamaged()                   { damageFlash = 8; }
    @Override public void onProgressChanged()           { repaint(); }
    @Override public void onRoomChanged(RoomPanel room, int index, int count) {
        roomTitle = room.getRoomName();
        roomIndex = index;
        roomCount = count;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        RoomTheme.smooth(g2);
        RoomTheme t = RoomTheme.current();
        int w = getWidth(), h = getHeight();

        // ---- 바탕: 금속 테두리 + 법랑 판 ----
        g2.setPaint(t.metal(0, h));
        g2.fillRect(0, 0, w, h);
        t.enamel(g2, RoomTheme.plate(5, 4, w - 10, h - 9, 7));
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawLine(14, 7, w - 15, 7);
        t.pin(g2, 10, h / 2, 3);
        t.pin(g2, w - 11, h / 2, 3);

        GameManager gm = GameManager.getInstance();
        RoomPanel room = gm.getCurrentRoom();

        // ---- 왼쪽: 방 이름 + 진행도 ----
        g2.setFont(Theme.font(Font.BOLD, 20));
        g2.setColor(t.accentLight);
        g2.drawString("ROOM " + (roomIndex + 1) + "/" + roomCount + "  ·  " + roomTitle, 24, 28);
        if (room != null && room.getTotalPuzzles() > 0) {
            int total = room.getTotalPuzzles(), solved = room.getSolvedCount();
            int bx = 24, by = 39, bw = 150, bh = 9;
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRoundRect(bx, by, bw, bh, 8, 8);
            g2.setPaint(new GradientPaint(bx, by, t.accent, bx + bw, by, t.accentLight));
            g2.fillRoundRect(bx, by, bw * solved / total, bh, 8, 8);
            g2.setColor(t.accent);
            g2.drawRoundRect(bx, by, bw, bh, 8, 8);
            g2.setFont(Theme.font(Font.PLAIN, 14));
            g2.setColor(t.textDim);
            PlayerStatus s = gm.getStatus();
            String who = s == null ? "" : "   ·   " + s.getName() + " (" + s.getDifficulty().getLabel() + ")";
            g2.drawString("퍼즐 " + solved + " / " + total + who, bx + bw + 10, by + 10);
        }

        // ---- 가운데: 타이머 (계기판 창) ----
        String time = GameTimer.format(remainingSec);
        g2.setFont(Theme.font(Font.BOLD, 34));
        int tw = g2.getFontMetrics().stringWidth(time);
        int cx = w / 2 - 40;
        Shape dial = RoomTheme.plate(cx - tw / 2 - 18, 8, tw + 36, h - 17, 7);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.fill(dial);
        g2.setColor(t.accent);
        g2.draw(dial);
        boolean danger = remainingSec <= 60, warn = remainingSec <= 300;
        g2.setColor(danger ? (blink ? t.heart : new Color(255, 160, 150))
                           : warn ? new Color(255, 190, 90) : t.accentLight);
        g2.drawString(time, cx - tw / 2, 44);
        g2.setFont(Theme.font(Font.PLAIN, 15));
        g2.setColor(t.textDim);
        g2.drawString("남은 시간", cx - tw / 2 - 90, 38);

        // ---- 오른쪽: 체력 하트 (버튼 왼쪽) ----
        int heartSize = 23, gap = 5;
        int heartsRight = getComponent(0).getX() - 16;
        int hx = heartsRight - maxHp * (heartSize + gap);
        g2.setFont(Theme.font(Font.PLAIN, 15));
        g2.setColor(t.textDim);
        g2.drawString("체력", hx - 40, 38);
        for (int i = 0; i < maxHp; i++) {
            boolean full = i < hp;
            boolean flashing = damageFlash > 0 && i == hp && damageFlash % 2 == 0;
            drawHeart(g2, hx + i * (heartSize + gap), 20, heartSize,
                flashing ? Color.WHITE : full ? t.heart : new Color(0, 0, 0, 110));
        }
        g2.dispose();
    }

    private void drawHeart(Graphics2D g2, int x, int y, int s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + s / 2.0, y + s);
        p.curveTo(x - s * 0.1, y + s * 0.55, x + s * 0.05, y - s * 0.15, x + s / 2.0, y + s * 0.25);
        p.curveTo(x + s * 0.95, y - s * 0.15, x + s * 1.1, y + s * 0.55, x + s / 2.0, y + s);
        g2.setColor(c);
        g2.fill(p);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(p);
    }

    /** 상단 바 오른쪽의 테마 버튼 (금속 판 모양) */
    private static final class BarButton extends JButton {
        BarButton(String text, String tip, Runnable action) {
            super(text);
            setToolTipText(tip);
            setFont(Theme.font(Font.BOLD, 16));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addActionListener(e -> {
                SoundManager.getInstance().play(SoundManager.Effect.CLICK);
                action.run();
            });
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            RoomTheme.smooth(g2);
            RoomTheme t = RoomTheme.current();
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover(), down = getModel().isPressed();
            t.chassis(g2, RoomTheme.plate(1, 1, w - 3, h - 4, 6));
            Shape face = RoomTheme.plate(4, 4, w - 9, h - 10, 4);
            g2.setPaint(new GradientPaint(0, 4, down ? t.enamelBottom : t.enamelTop, 0, h - 6,
                down ? t.enamelTop : t.enamelBottom));
            g2.fill(face);
            if (hover) {
                g2.setColor(new Color(t.accentLight.getRed(), t.accentLight.getGreen(), t.accentLight.getBlue(), 55));
                g2.fill(face);
            }
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            String s = getText();
            g2.setColor(hover ? Color.WHITE : t.accentLight);
            g2.drawString(s, (w - fm.stringWidth(s)) / 2, (h - 4 + fm.getAscent() - fm.getDescent()) / 2 + (down ? 1 : 0));
            g2.dispose();
        }
    }
}
