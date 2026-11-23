package common;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 힌트 창. 현재 방의 진행 단계에 맞는 힌트를 약한 것부터 하나씩 공개한다.
 * 새 힌트를 열 때마다 힌트 1개 + 제한 시간이 차감되며, 이미 본 힌트는 무료로 다시 볼 수 있다.
 */
public class HintDialog extends DialogBox {
    private final JPanel listPanel;
    private final JButton revealButton;

    public HintDialog(Window owner) {
        super(owner, "힌트", 560, 440);

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Theme.BG_PANEL);
        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(Theme.goldBorder(1));
        setContent(sp);

        revealButton = addButton("", this::reveal);
        addButton("닫기", this::dispose);
        refresh();
    }

    private void reveal() {
        GameManager gm = GameManager.getInstance();
        PlayerStatus s = gm.getStatus();
        if (s.getHintsLeft() <= 0) {
            DialogBox.message(this, "힌트 없음", "남은 힌트가 없습니다.\n스스로의 힘으로 풀어보세요!");
            return;
        }
        if (GameSettings.getInstance().isConfirmHint()) {
            int penalty = s.getDifficulty().getHintPenaltySec();
            boolean ok = DialogBox.confirm(this, "힌트 사용",
                "힌트를 1개 사용합니다.\n남은 시간이 " + penalty + "초 줄어듭니다.\n계속할까요?");
            if (!ok) return;
        }
        gm.revealNextHint();
        if (gm.isRunning()) refresh();
    }

    private void refresh() {
        GameManager gm = GameManager.getInstance();
        List<String> revealed = gm.getRevealedHints();
        listPanel.removeAll();

        if (revealed.isEmpty()) {
            listPanel.add(row("아직 공개한 힌트가 없습니다.\n막혔다면 아래 버튼으로 힌트를 열어보세요.", Theme.TEXT_DIM, 0));
        }
        for (int i = 0; i < revealed.size(); i++) {
            listPanel.add(row(revealed.get(i), Theme.TEXT, i + 1));
        }
        listPanel.revalidate();
        listPanel.repaint();

        PlayerStatus s = gm.getStatus();
        boolean more = gm.hasMoreHints();
        revealButton.setText(more
            ? "다음 힌트 열기 (남은 힌트 " + s.getHintsLeft() + "개, -" + s.getDifficulty().getHintPenaltySec() + "초)"
            : "이 단계의 힌트를 모두 열었습니다");
        revealButton.setEnabled(more && s.getHintsLeft() > 0);
    }

    private JComponent row(String text, Color color, int number) {
        JTextArea a = new JTextArea((number > 0 ? "힌트 " + number + ".  " : "") + text);
        a.setEditable(false);
        a.setFocusable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setFont(Theme.font(number > 0 ? Font.BOLD : Font.PLAIN, 15));
        a.setForeground(color);
        a.setBackground(Theme.BG_PANEL);
        a.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(90, 70, 45)),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        a.setAlignmentX(Component.LEFT_ALIGNMENT);
        return a;
    }
}
