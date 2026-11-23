package common;

import javax.swing.*;
import java.awt.*;

/**
 * 테마가 적용된 공통 팝업 창의 부모 클래스.
 * 상단 제목 + 가운데 내용 + 하단 버튼줄 구조이며,
 * HintDialog / MemoDialog / SettingsDialog / HelpDialog / RankingDialog / DevModeDialog 가 상속한다.
 */
public class DialogBox extends JDialog {
    protected final JPanel contentArea;
    protected final JPanel buttonRow;

    public DialogBox(Window owner, String title, int width, int height) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);   // 닫으면 완전히 닫히게 (닫힐 때 일시정지 해제 등이 실행됨)
        setUndecorated(true);                         // 운영체제 창 테두리 없이 게임 테마 모양만
        RoomPopup.dimWhileOpen(this);                 // 열려 있는 동안 뒤의 게임 화면을 어둡게
        setSize(width, height);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBackground(Theme.BG_DARK);
        root.setBorder(BorderFactory.createCompoundBorder(
            Theme.goldBorder(2), BorderFactory.createEmptyBorder(16, 20, 14, 20)));

        JLabel header = Theme.label("【 " + title + " 】", Font.BOLD, 22, Theme.GOLD_LIGHT);
        header.setHorizontalAlignment(SwingConstants.CENTER);
        root.add(header, BorderLayout.NORTH);

        contentArea = new JPanel(new BorderLayout());
        contentArea.setOpaque(false);
        root.add(contentArea, BorderLayout.CENTER);

        buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonRow.setOpaque(false);
        root.add(buttonRow, BorderLayout.SOUTH);

        setContentPane(root);

        // ESC 로 닫기
        getRootPane().registerKeyboardAction(e -> dispose(),
            KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** 창의 빈 곳(제목 · 여백)을 마우스로 끌어서 창을 옮길 수 있게 한다 */
    protected void enableDragMove() {
        java.awt.event.MouseAdapter drag = new java.awt.event.MouseAdapter() {
            private Point grab;
            @Override public void mousePressed(java.awt.event.MouseEvent e) { grab = e.getPoint(); }
            @Override public void mouseDragged(java.awt.event.MouseEvent e) {
                if (grab == null) return;
                Point p = e.getLocationOnScreen();
                setLocation(p.x - grab.x, p.y - grab.y);
            }
        };
        JComponent root = (JComponent) getContentPane();
        root.addMouseListener(drag);
        root.addMouseMotionListener(drag);
        root.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
    }

    protected void setContent(JComponent c) {
        contentArea.removeAll();
        contentArea.add(c, BorderLayout.CENTER);
        contentArea.revalidate();
    }

    protected JButton addButton(String text, Runnable action) {
        JButton b = Theme.button(text, 15);
        b.addActionListener(e -> action.run());
        buttonRow.add(b);
        return b;
    }

    /** 여러 줄 텍스트를 보여주는 읽기 전용 영역 */
    protected static JScrollPane textArea(String text, int fontSize) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.font(Font.PLAIN, fontSize));
        area.setBackground(Theme.BG_PANEL);
        area.setForeground(Theme.TEXT);
        area.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        area.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(Theme.goldBorder(1));
        return sp;
    }

    // ------------------------------------------------------------------
    // 간단한 알림 / 확인 창
    // ------------------------------------------------------------------

    /** 글이 길면 창을 글에 맞게 넓히고 높인다 (최소 크기 minW x minH) */
    private static void fitToText(DialogBox d, JComponent text, int minW, int minH) {
        Dimension p = text.getPreferredSize();
        d.setSize(Math.max(minW, p.width + 90), Math.max(minH, p.height + 170));
        d.setLocationRelativeTo(d.getOwner());
    }

    public static void message(Component parent, String title, String text) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        DialogBox d = new DialogBox(owner, title, 460, 260);
        JLabel l = Theme.label("<html><div style='text-align:center'>" + text.replace("\n", "<br>")
            + "</div></html>", Font.PLAIN, 16, Theme.TEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        d.setContent(l);
        fitToText(d, l, 460, 260);
        d.addButton("확인", d::dispose);
        d.setVisible(true);
    }

    public static boolean confirm(Component parent, String title, String text) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        DialogBox d = new DialogBox(owner, title, 480, 270);
        JLabel l = Theme.label("<html><div style='text-align:center'>" + text.replace("\n", "<br>")
            + "</div></html>", Font.PLAIN, 16, Theme.TEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        d.setContent(l);
        fitToText(d, l, 480, 270);
        final boolean[] ok = {false};
        d.addButton("예", () -> { ok[0] = true; d.dispose(); });
        d.addButton("아니오", d::dispose);
        d.setVisible(true);
        return ok[0];
    }
}
