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

    private Brass() {}

    static Font font(int style, int size) {
        Font korean = new Font("Malgun Gothic", style, size);
        if (!korean.getFamily().equals(Font.DIALOG) && korean.canDisplay('한')) return korean;
        return UIManager.getFont("Label.font").deriveFont(style, (float) size);
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

    static JPanel row() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row.setOpaque(false);
        return row;
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
                Color top = lit ? new Color(105, 79, 48) : new Color(69, 55, 40);
                Color bottom = new Color(42, 33, 26);
                if (getModel().isArmed()) top = bottom;
                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 7, 7);
                g.setColor(lit ? GOLD : EDGE);
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 7, 7);
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

    static JButton switchKey(String label, boolean emergency, Runnable action) {
        JButton key = new JButton(label) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                smooth(g);
                int cx = getWidth()/2, y = getModel().isPressed() ? 7 : 4;
                g.setPaint(new GradientPaint(0, 3, GOLD, 0, 45, BROWN));
                g.fillOval(cx-22, 2, 44, 44);
                g.setColor(INK);
                g.fillOval(cx-19, 5, 38, 38);
                Color light = emergency ? new Color(224, 100, 76) : new Color(235, 207, 152);
                Color dark = emergency ? new Color(112, 34, 27) : new Color(138, 98, 48);
                g.setPaint(new GradientPaint(cx, y, light, cx, y+32, dark));
                g.fillOval(cx-17, y+2, 34, 33);
                g.setColor(isFocusOwner() || getModel().isRollover() ? Color.WHITE : CREAM);
                g.setFont(getFont());
                int width = g.getFontMetrics().stringWidth(getText());
                g.drawString(getText(), cx-width/2, 66);
                if (isFocusOwner()) g.drawRoundRect(1, 1, getWidth()-3, getHeight()-3, 8, 8);
                g.dispose();
            }
        };
        key.setPreferredSize(new Dimension(85, 74));
        key.setFont(font(Font.BOLD, 14));
        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        key.addActionListener(e -> action.run());
        return key;
    }
}
