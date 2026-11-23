package common;

import javax.swing.*;
import java.awt.*;

/**
 * 일시정지 메뉴 (ESC). 열려 있는 동안 제한 시간이 멈춘다.
 */
public class PauseDialog extends DialogBox {

    public PauseDialog(MainFrame frame) {
        super(frame, "일시정지", 360, 470);
        GameManager gm = GameManager.getInstance();

        JPanel col = new JPanel(new GridLayout(0, 1, 0, 10));
        col.setOpaque(false);
        col.setBorder(BorderFactory.createEmptyBorder(6, 30, 6, 30));

        JLabel info = Theme.label("남은 시간 " + GameTimer.format(gm.getRemainingSec()) + "  (정지됨)",
            Font.PLAIN, 14, Theme.TEXT_DIM);
        info.setHorizontalAlignment(SwingConstants.CENTER);
        col.add(info);

        col.add(item("계속하기", this::dispose));
        col.add(item("설정", () -> new SettingsDialog(this).setVisible(true)));
        col.add(item("도움말", () -> new HelpDialog(this).setVisible(true)));
        col.add(item("처음부터 다시하기", () -> {
            if (DialogBox.confirm(this, "다시하기", "현재 진행 상황을 버리고\n처음부터 다시 시작할까요?")) {
                dispose();
                gm.restart();
            }
        }));
        col.add(item("저장 후 타이틀로", () -> {
            dispose();
            gm.quitToTitle(true);
        }));
        col.add(item("게임 종료", () -> {
            if (DialogBox.confirm(this, "게임 종료", "진행 상황을 저장하고 게임을 종료할까요?")) {
                SaveManager.save(gm);
                System.exit(0);
            }
        }));
        setContent(col);

        gm.pause();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                gm.resume();
            }
        });
    }

    private JButton item(String text, Runnable r) {
        JButton b = Theme.button(text, 16);
        b.addActionListener(e -> r.run());
        return b;
    }
}
