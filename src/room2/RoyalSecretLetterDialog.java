package room2;

import javax.swing.*;
import java.awt.*;

public class RoyalSecretLetterDialog extends JDialog {

    public RoyalSecretLetterDialog(Window parent) {
        super(parent, "왕의 밀서", ModalityType.APPLICATION_MODAL);
        setSize(540, 720);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel() {
            private Image parchmentImg = new ImageIcon("resources/room2/왕위_계승_기록.png").getImage();
            private Font titleFont = new Font("바탕", Font.BOLD, 24);
            private Font bodyFont = new Font("바탕", Font.BOLD, 17);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (parchmentImg != null) {
                    g2d.drawImage(parchmentImg, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2d.setColor(new Color(235, 215, 175));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }

                String[] lines = {
                    "[ 왕의 밀서 ]",
                    "",
                    "① 곰은 왕좌 곁에 앉지 않는다.",
                    "",
                    "② 곰과 사자는 원탁을 사이에 두고",
                    "서로 마주 본다.",
                    "",
                    "③ 사슴은 늑대의 시계 방향",
                    "바로 다음 자리에 앉는다.",
                    "",
                    "④ 늑대는 사자와 이웃해 앉지 않는다.",
                    "",
                    "⑤ 왕좌와 마주 보는 기사가",
                    "나의 기사다."
                };

                int startY = 200;
                int lineHeight = 28;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty()) continue;

                    g2d.setFont(i == 0 ? titleFont : bodyFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    int textWidth = fm.stringWidth(line);
                    int startX = (getWidth() - textWidth) / 2;
                    int currentY = startY + (i * lineHeight);

                    g2d.setColor(new Color(90, 60, 35, 70));
                    g2d.drawString(line, startX + 1, currentY + 1);

                    // 5번 핵심 단서는 붉은 잉크색으로 강조
                    if (i >= 12) {
                        g2d.setColor(new Color(130, 25, 20));
                    } else {
                        g2d.setColor(new Color(40, 25, 15));
                    }
                    g2d.drawString(line, startX, currentY);
                }

                g2d.dispose();
            }
        };

        add(panel);
    }
}