package common;

import javax.swing.*;
import java.awt.*;

/**
 * 메모장. 단서를 적어둘 수 있으며 내용은 저장 파일에도 함께 저장된다.
 */
public class MemoDialog extends DialogBox {

    public MemoDialog(Window owner) {
        super(owner, "메모장", 520, 480);
        PlayerStatus status = GameManager.getInstance().getStatus();

        JTextArea area = new JTextArea(status.getMemo());
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.font(Font.PLAIN, 16));
        area.setBackground(new Color(235, 220, 185));     // 양피지 색
        area.setForeground(new Color(45, 28, 15));
        area.setCaretColor(new Color(45, 28, 15));
        area.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(Theme.goldBorder(1));

        JLabel tip = Theme.label("단서나 추리 내용을 자유롭게 적어두세요. 창을 닫으면 자동 저장됩니다.",
            Font.PLAIN, 12, Theme.TEXT_DIM);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.setOpaque(false);
        p.add(tip, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        setContent(p);

        addButton("지우기", () -> area.setText(""));
        addButton("닫기", this::dispose);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                status.setMemo(area.getText());
            }
        });
    }
}
