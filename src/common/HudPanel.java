package common;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * 화면 상단 상태바(HUD): 방 이름 / 진행도 / 체력(하트) / 남은 시간 / 남은 힌트.
 * GameListener 를 구현하여 상태가 바뀌면 자동으로 다시 그린다.
 */
public class HudPanel extends JPanel implements GameListener {
    public static final int HEIGHT = 54;

    private String roomTitle = "";
    private int roomIndex, roomCount;
    private int hp, maxHp;
    private int remainingSec;
    private int hintsLeft;
    private int damageFlash;     // 피격 시 하트 깜빡임
    private boolean blink;       // 1분 이하 타이머 깜빡임
    private final Timer animTimer;

    public HudPanel() {
        setPreferredSize(new Dimension(RoomPanel.ROOM_WIDTH + SideMenuPanel.WIDTH, HEIGHT));
        setBackground(Theme.BG_DARK);
        animTimer = new Timer(120, e -> {
            if (damageFlash > 0) damageFlash--;
            blink = !blink;
            repaint();
        });
        animTimer.start();
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
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, new Color(48, 34, 22), 0, h, Theme.BG_DARK));
        g2.fillRect(0, 0, w, h);
        g2.setColor(Theme.GOLD);
        g2.fillRect(0, h - 2, w, 2);

        GameManager gm = GameManager.getInstance();
        RoomPanel room = gm.getCurrentRoom();

        // ---- 왼쪽: 방 이름 + 진행도 ----
        g2.setFont(Theme.font(Font.BOLD, 18));
        g2.setColor(Theme.GOLD_LIGHT);
        g2.drawString(String.format("ROOM %d/%d  ·  %s", roomIndex + 1, roomCount, roomTitle), 18, 24);
        if (room != null && room.getTotalPuzzles() > 0) {
            int total = room.getTotalPuzzles(), solved = room.getSolvedCount();
            int bx = 18, by = 33, bw = 220, bh = 9;
            g2.setColor(new Color(60, 45, 30));
            g2.fillRoundRect(bx, by, bw, bh, 8, 8);
            g2.setColor(Theme.SUCCESS);
            g2.fillRoundRect(bx, by, bw * solved / total, bh, 8, 8);
            g2.setColor(Theme.GOLD);
            g2.drawRoundRect(bx, by, bw, bh, 8, 8);
            g2.setFont(Theme.font(Font.PLAIN, 12));
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString("퍼즐 " + solved + " / " + total, bx + bw + 10, by + 9);
        }

        // ---- 가운데: 타이머 ----
        boolean danger = remainingSec <= 60;
        boolean warn = remainingSec <= 300;
        g2.setFont(Theme.font(Font.BOLD, 30));
        String time = GameTimer.format(remainingSec);
        int tw = g2.getFontMetrics().stringWidth(time);
        int tx = (w - tw) / 2;
        g2.setColor(danger ? (blink ? Theme.DANGER : new Color(255, 150, 140))
                           : warn ? new Color(255, 190, 90) : Theme.GOLD_LIGHT);
        g2.drawString(time, tx, 38);
        g2.setFont(Theme.font(Font.PLAIN, 12));
        g2.setColor(Theme.TEXT_DIM);
        g2.drawString("남은 시간", tx - 62, 34);

        // ---- 오른쪽: 체력 하트 + 힌트 ----
        int heartSize = 22, gap = 5;
        int heartsW = maxHp * (heartSize + gap);
        int hx = w - heartsW - 150;
        g2.setFont(Theme.font(Font.PLAIN, 12));
        g2.setColor(Theme.TEXT_DIM);
        g2.drawString("체력", hx - 34, 32);
        for (int i = 0; i < maxHp; i++) {
            boolean full = i < hp;
            boolean flashing = damageFlash > 0 && i == hp && damageFlash % 2 == 0;
            drawHeart(g2, hx + i * (heartSize + gap), 15, heartSize,
                flashing ? Color.WHITE : full ? Theme.DANGER : new Color(70, 50, 45));
        }

        g2.setFont(Theme.font(Font.BOLD, 16));
        g2.setColor(hintsLeft > 0 ? Theme.GOLD_LIGHT : Theme.TEXT_DIM);
        g2.drawString("힌트 " + hintsLeft + "개", w - 120, 33);

        g2.dispose();
    }

    private void drawHeart(Graphics2D g2, int x, int y, int s, Color c) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + s / 2.0, y + s);
        p.curveTo(x - s * 0.1, y + s * 0.55, x + s * 0.05, y - s * 0.15, x + s / 2.0, y + s * 0.25);
        p.curveTo(x + s * 0.95, y - s * 0.15, x + s * 1.1, y + s * 0.55, x + s / 2.0, y + s);
        g2.setColor(c);
        g2.fill(p);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.setStroke(new BasicStroke(1.2f));
        g2.draw(p);
    }
}
