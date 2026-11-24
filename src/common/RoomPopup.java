package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 방 안의 단서 · 퍼즐 창을 게임 화면 위에 뜨는 팝업처럼 보이게 하는 도우미.
 *  - 운영체제 창 테두리(흰 제목줄, X 버튼) 없이 그림만 보인다
 *  - 그림의 투명한 부분(두루마리 가장자리 등)은 비쳐 보인다
 *  - 열려 있는 동안 뒤의 게임 화면을 어둡게 깐다
 *  - 왼쪽 위 [← 돌아가기] 버튼(방 테마 모양) 또는 ESC 로 닫는다
 *
 * 사용법: 대화창 생성자 마지막에 {@code RoomPopup.style(this);}
 */
public final class RoomPopup {
    private RoomPopup() {}

    private static int openCount;
    private static JComponent dim;

    /** 창을 화면에 띄우기 전에(생성자 안에서) 호출한다 */
    public static void style(JDialog d) {
        if (d.isDisplayable()) return;
        d.setUndecorated(true);
        d.setBackground(new Color(0, 0, 0, 0));
        Container content = d.getContentPane();
        if (content instanceof JComponent c) c.setOpaque(false);
        for (Component child : content.getComponents()) {
            if (child instanceof JPanel p) p.setOpaque(false);
        }

        JButton back = new ThemedButton("← 돌아가기", d::dispose);
        d.getLayeredPane().add(back, JLayeredPane.PALETTE_LAYER);
        d.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { placeBack(d, back); }
            @Override public void componentShown(ComponentEvent e) { placeBack(d, back); }
        });
        placeBack(d, back);

        d.getRootPane().registerKeyboardAction(e -> d.dispose(),
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dimWhileOpen(d);
    }

    private static void placeBack(JDialog d, JButton back) {
        Dimension p = back.getPreferredSize();
        back.setBounds(12, 12, p.width, p.height);
    }

    /** 모달 창이 열려 있는 동안 게임 창을 어둡게 한다 (창이 여러 겹이어도 한 번만) */
    static void dimWhileOpen(Window w) {
        w.addWindowListener(new WindowAdapter() {
            private boolean counted;
            @Override public void windowOpened(WindowEvent e) {
                if (w instanceof Dialog dialog && !dialog.isModal()) return;
                counted = true;
                if (openCount++ == 0) setDimmed(true);
            }
            @Override public void windowClosed(WindowEvent e) {
                if (!counted) return;
                counted = false;
                if (--openCount <= 0) { openCount = 0; setDimmed(false); }
            }
        });
    }

    private static void setDimmed(boolean on) {
        MainFrame frame = GameManager.getInstance().getFrame();
        if (frame == null) return;
        JLayeredPane layers = frame.getLayeredPane();
        if (dim == null) {
            dim = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    g.setColor(new Color(0, 0, 0, 150));
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                @Override public boolean contains(int x, int y) { return false; }
            };
        }
        if (on) {
            dim.setBounds(0, 0, layers.getWidth(), layers.getHeight());
            layers.add(dim, JLayeredPane.MODAL_LAYER);
        } else {
            layers.remove(dim);
        }
        layers.repaint();
    }
}
