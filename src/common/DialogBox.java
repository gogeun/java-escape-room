package common;

import javax.swing.*;
import java.awt.*;

/**
 * 테마가 적용된 공통 팝업 창의 부모 클래스.
 * 상단 제목 + 가운데 내용 + 하단 버튼줄 구조이며,
 * HintDialog / MemoDialog / PauseDialog / SettingsDialog / HelpDialog / RankingDialog 가 상속한다.
 */
public class DialogBox extends JDialog {
    protected final JPanel contentArea;
    protected final JPanel buttonRow;

    public DialogBox(Window owner, String title, int width, int height) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
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

    public static void message(Component parent, String title, String text) {
        Window owner = parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent);
        DialogBox d = new DialogBox(owner, title, 460, 260);
        JLabel l = Theme.label("<html><div style='text-align:center'>" + text.replace("\n", "<br>")
            + "</div></html>", Font.PLAIN, 16, Theme.TEXT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        d.setContent(l);
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
        final boolean[] ok = {false};
        d.addButton("예", () -> { ok[0] = true; d.dispose(); });
        d.addButton("아니오", d::dispose);
        d.setVisible(true);
        return ok[0];
    }
}
