package common;

import javax.swing.*;
import java.awt.*;

/**
 * 현재 방 테마(RoomTheme)의 금속 판 모양 버튼.
 * 상단 바 버튼과 같은 모양으로, 방 화면 위에 띄우는 버튼(예: 3번 방 조사 화면의 "돌아가기")에 쓴다.
 */
public class ThemedButton extends JButton {

    public ThemedButton(String text, Runnable action) {
        super(text);
        setFont(Theme.font(Font.BOLD, 16));
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setFocusable(false);
        setRolloverEnabled(true);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        FontMetrics fm = getFontMetrics(getFont());
        setPreferredSize(new Dimension(fm.stringWidth(text) + 40, 42));
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
        t.chassis(g2, RoomTheme.plate(1, 1, w - 3, h - 4, 7));
        Shape face = RoomTheme.plate(4, 4, w - 9, h - 10, 5);
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
