package common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * 게임 전체에서 공통으로 사용하는 색상 / 폰트 / 버튼 스타일 모음.
 * 기본은 어두운 갈색 + 금색이고, 방에 들어가면 apply(RoomTheme) 로 그 방 테마 색으로 바뀐다.
 * (팝업 창은 만들어질 때의 색을 쓰므로 방마다 다른 색의 팝업이 뜬다)
 */
public final class Theme {
    private Theme() {}

    public static Color BG_DARK      = new Color(22, 17, 13);
    public static Color BG_PANEL     = new Color(38, 29, 22);
    public static Color BG_BUTTON    = new Color(70, 45, 25);
    public static Color BG_BUTTON_HOVER = new Color(105, 68, 35);
    public static Color GOLD         = new Color(205, 165, 85);
    public static Color GOLD_LIGHT   = new Color(255, 230, 160);
    public static Color TEXT         = new Color(240, 225, 195);
    public static Color TEXT_DIM     = new Color(170, 150, 120);
    public static final Color DANGER       = new Color(220, 60, 50);
    public static final Color SUCCESS      = new Color(110, 190, 110);

    public static final String FONT_NAME = "바탕";

    /** 방 테마 색으로 바꾼다 (MainFrame 이 방 이동 / 타이틀 복귀 때 호출) */
    public static void apply(RoomTheme t) {
        BG_DARK = t.bgDark;
        BG_PANEL = t.bgPanel;
        BG_BUTTON = t.bgButton;
        BG_BUTTON_HOVER = t.bgButtonHover;
        GOLD = t.accent;
        GOLD_LIGHT = t.accentLight;
        TEXT = t.text;
        TEXT_DIM = t.textDim;
    }

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
