package common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 게임 전체에서 공통으로 사용하는 색상 / 폰트 / 버튼 스타일 모음.
 * (중세 방탈출 분위기에 맞춘 어두운 갈색 + 금색 테마)
 */
public final class Theme {
    private Theme() {}

    public static final Color BG_DARK      = new Color(22, 17, 13);
    public static final Color BG_PANEL     = new Color(38, 29, 22);
    public static final Color BG_BUTTON    = new Color(70, 45, 25);
    public static final Color BG_BUTTON_HOVER = new Color(105, 68, 35);
    public static final Color GOLD         = new Color(205, 165, 85);
    public static final Color GOLD_LIGHT   = new Color(255, 230, 160);
    public static final Color TEXT         = new Color(240, 225, 195);
    public static final Color TEXT_DIM     = new Color(170, 150, 120);
    public static final Color DANGER       = new Color(220, 60, 50);
    public static final Color SUCCESS      = new Color(110, 190, 110);

    public static final String FONT_NAME = "바탕";

    public static Font font(int style, int size) {
        return new Font(FONT_NAME, style, size);
    }

    public static Border goldBorder(int thickness) {
        return BorderFactory.createLineBorder(GOLD, thickness);
    }

    /** 테마가 적용된 버튼 생성 (마우스 올리면 밝아짐 + 클릭음) */
    public static JButton button(String text, int fontSize) {
        JButton btn = new JButton(text);
        btn.setFont(font(Font.BOLD, fontSize));
        btn.setBackground(BG_BUTTON);
        btn.setForeground(GOLD_LIGHT);
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            goldBorder(1), BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(BG_BUTTON_HOVER);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(BG_BUTTON);
            }
        });
        btn.addActionListener(e -> SoundManager.getInstance().play(SoundManager.Effect.CLICK));
        return btn;
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }
}
