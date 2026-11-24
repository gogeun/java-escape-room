package room1;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/** 금속 테두리, 크림색 활자, 종이 문서 등 기존 기관실의 팔레트. */
final class Brass {
    static final Color INK = new Color(25, 22, 18), PLATE = new Color(42, 35, 28);
    static final Color CREAM = new Color(245, 230, 200), GOLD = new Color(223, 185, 116);
    static final Color EDGE = new Color(142, 111, 70), MUTED = new Color(205, 187, 153);
    static final Color PAPER = new Color(232, 215, 180), BROWN = new Color(62, 46, 29);
    static final Color GREEN = new Color(149, 207, 151);
    private static final Font BASE = baseFont();
    private static final Font[][] FONTS = new Font[4][96];

    private Brass() {}

    private static Font baseFont() {
        Font korean = new Font("Malgun Gothic", Font.PLAIN, 14);
        if (!korean.getFamily().equals(Font.DIALOG) && korean.canDisplay('한')) return korean;
        return UIManager.getFont("Label.font");
    }

    static Font font(int style, int size) {
        if (size >= FONTS[style].length) return BASE.deriveFont(style, (float)size);
        Font font = FONTS[style][size];
        if (font == null) FONTS[style][size] = font = BASE.deriveFont(style, (float)size);
        return font;
    }

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    static JLabel label(String s, int size, Color color, boolean bold) {
        JLabel label = new JLabel(s);
        label.setFont(font(bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(color);
        return label;
    }

    static Border frame(int pad) {
        return new CompoundBorder(new LineBorder(EDGE), new EmptyBorder(pad, pad, pad, pad));
    }

    static JButton button(String text, Runnable action) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                smooth(g);
                boolean lit = getModel().isRollover() || isFocusOwner();
                int w = getWidth(), h = getHeight();
                if (w > 10 && h > 8) {
                    HudMetal.chassis(g, HudMetal.plate(1, 1, w-3, h-4, 5));
                    Color top = lit ? new Color(85, 112, 94) : new Color(56, 78, 69);
                    Color bottom = new Color(24, 39, 38);
                    g.setPaint(new GradientPaint(0, 2, getModel().isArmed() ? bottom : top, 0, h, bottom));
                    g.fill(HudMetal.plate(3, 3, w-7, h-8, 4));
                    g.setColor(lit ? GOLD : new Color(164, 151, 110));
                    g.drawLine(8, 4, w-9, 4);
                }
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        b.setFont(font(Font.BOLD, 14));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 12, 9, 12));
        b.setForeground(CREAM);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

}
