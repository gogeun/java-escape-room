package room2;

import javax.swing.*;
import java.awt.*;

public class ScrollPopupDialog extends JDialog {

    public ScrollPopupDialog(Window parent) {
        super(parent, "왕위 계승 기록", ModalityType.APPLICATION_MODAL);
        setSize(540, 720);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel popupPanel = new JPanel() {
            private Image popupImg = new ImageIcon("resources/room2/왕위_계승_기록.png").getImage();
            private Font titleFont = new Font("바탕", Font.BOLD, 25);
            private Font bodyFont = new Font("바탕", Font.BOLD, 22);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (popupImg != null) {
                    g2d.drawImage(popupImg, 0, 0, getWidth(), getHeight(), this);
                }

                String[] lines = {
                    "[ 왕실 계승 기록 ]",
                    "",
                    "세른의 왕관은 곧바로",
                    "베른에게 넘어갔다.",
                    "아렌은 다렌보다 먼저",
                    "왕위에 올랐지만,",
                    "다렌에게 직접 왕관을",
                    "넘기지는 못했다."
                };

                int startY = 230;   // 상단 왕관 문장 바로 아래부터 시작
                int lineHeight = 42; // 넉넉한 줄 간격

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.isEmpty()) continue;

                    // 첫 줄(제목)은 조금 더 크게 설정
                    g2d.setFont(i == 0 ? titleFont : bodyFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    int textWidth = fm.stringWidth(line);
                    int startX = (getWidth() - textWidth) / 2;
                    int currentY = startY + (i * lineHeight);

                    // 양피지 잉크 질감을 위한 미세 그림자
                    g2d.setColor(new Color(90, 60, 35, 70));
                    g2d.drawString(line, startX + 1, currentY + 1);

                    // 본문 잉크 색상 (짙은 갈색)
                    g2d.setColor(new Color(40, 25, 15));
                    g2d.drawString(line, startX, currentY);
                }

                g2d.dispose();
            }
        };

        add(popupPanel);
    }
}