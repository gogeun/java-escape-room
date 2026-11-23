package room2;

import javax.swing.*;
import java.awt.*;

public class RoyalPassDialog extends JDialog {

    public RoyalPassDialog(Window parent) {
        super(parent, "왕실 인증패 (뒷면)", ModalityType.APPLICATION_MODAL);
        setSize(680, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel() {
            private Image passImg = new ImageIcon("resources/room2/왕실_인증패.png").getImage();
            private Font titleFont = new Font("바탕", Font.BOLD, 17);
            private Font clueFont = new Font("바탕", Font.BOLD, 16);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2d.setColor(new Color(25, 20, 18));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                if (passImg != null) {
                    g2d.drawImage(passImg, 0, 0, getWidth(), getHeight(), this);
                }

                String[] lines = {
                    "",
                    "① 짐승은 하나뿐이다.",
                    "② 독수리가 있으면 검도 있다.",
                    "③ 검이 있으면 방패도 있다.",
                    "④ 방패가 있으면 장미도 있다.",
                    "⑤ 장미가 있으면 왕관도 있다."
                };

                int startY = 232;   // 인증패 내 빈 양피지 영역 시작 Y좌표
                int lineHeight = 25; // 줄 간격

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    g2d.setFont(i == 0 ? titleFont : clueFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    int textWidth = fm.stringWidth(line);
                    int startX = (getWidth() - textWidth) / 2;
                    int currentY = startY + (i * lineHeight);

                    g2d.setColor(new Color(90, 60, 35, 70));
                    g2d.drawString(line, startX + 1, currentY + 1);

                    g2d.setColor(new Color(40, 25, 15));
                    g2d.drawString(line, startX, currentY);
                }

                g2d.dispose();
            }
        };

        add(panel);
    }
}