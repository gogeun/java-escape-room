package common;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * 조사할 수 있는 물건 위에 띄우는 돋보기 표시 (1번 방의 magnifier.png / magnifier-hover.png 를 함께 사용).
 *  - 평소에는 돋보기, 마우스를 올리면 밝은 돋보기 + 물건 이름표
 *  - 2번 방: 투명 버튼(클릭 영역)에 decorate() 로 붙인다
 *  - 3번 방: 장면을 그릴 때 paint() 로 직접 그린다
 */
public final class Magnifier {
    private Magnifier() {}

    public static final int SIZE = 34;   // 1·2·3번 방 공통 돋보기 크기
    private static final BufferedImage NORMAL = load("magnifier.png");
    private static final BufferedImage HOVER = load("magnifier-hover.png");

    private static BufferedImage load(String name) {
        try {
            File f = Resources.file("resources/room1/" + name);
            return f.isFile() ? ImageIO.read(f) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** (cx, cy) 를 중심으로 돋보기를 그린다. 이미지가 없으면 간단한 원으로 대신 그린다 */
    public static void paint(Graphics2D g, double cx, double cy, double size, boolean active) {
        BufferedImage img = active ? HOVER : NORMAL;
        int s = (int) Math.round(size), x = (int) Math.round(cx - size / 2), y = (int) Math.round(cy - size / 2);
        if (img != null) {
            g.drawImage(img, x, y, s, s, null);
            return;
        }
        RoomTheme t = RoomTheme.current();
        g.setColor(new Color(0, 0, 0, 150));
        g.fillOval(x, y, s, s);
        g.setStroke(new BasicStroke(2f));
        g.setColor(active ? t.accentLight : t.accent);
        g.drawOval(x + s / 5, y + s / 5, s / 2, s / 2);
        g.drawLine(x + s * 13 / 20, y + s * 13 / 20, x + s * 4 / 5, y + s * 4 / 5);
        g.setStroke(new BasicStroke(1));
    }

    /** 돋보기 아래에 물건 이름표를 그린다 (1번 방과 같은 모양, 색은 현재 방 테마) */
    public static void paintLabel(Graphics2D g, String label, double cx, double cy, int areaWidth, int areaHeight) {
        if (label == null || label.isEmpty()) return;
        RoomTheme t = RoomTheme.current();
        g.setFont(Theme.font(Font.BOLD, 14));
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(label) + 14;
        int left = (int) Math.max(0, Math.min(areaWidth - w, cx - w / 2.0));
        int top = (int) cy + SIZE / 2 + 2;
        if (top + 26 > areaHeight) top = (int) cy - SIZE / 2 - 28;   // 아래 공간이 없으면 돋보기 위에
        g.setColor(new Color(t.bgDark.getRed(), t.bgDark.getGreen(), t.bgDark.getBlue(), 225));
        g.fillRoundRect(left, top, w, 25, 7, 7);
        g.setColor(t.accent);
        g.drawRoundRect(left, top, w, 25, 7, 7);
        g.setColor(t.text);
        g.drawString(label, left + 7, top + 18);
    }

    /**
     * 투명 버튼(클릭 영역)에 돋보기를 붙인다.
     * @param fx, fy 버튼 안에서 돋보기 위치 (0~1, 0.5 = 가운데)
     */
    public static void decorate(AbstractButton b, String label, double fx, double fy) {
        b.setUI(new BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                RoomTheme.smooth(g2);
                boolean active = b.getModel().isRollover() || b.getModel().isPressed();
                double cx = c.getWidth() * fx, cy = c.getHeight() * fy;
                Magnifier.paint(g2, cx, cy, SIZE, active);
                if (active) Magnifier.paintLabel(g2, label, cx, cy, c.getWidth(), c.getHeight());
                g2.dispose();
            }
        });
        b.setText("");
        b.setToolTipText(null);
        b.setRolloverEnabled(true);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
