package room2;

import javax.swing.*;
import java.awt.*;

public class KnightRewardDialog extends JDialog {

    public KnightRewardDialog(Window parent) {
        super(parent, "기사 임명장 및 늑대 문장 열쇠", ModalityType.APPLICATION_MODAL);
        setSize(860, 640);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel() {
            private Image certImg = Room2Panel.loadRoomImage("기사_임명장");
            private Image keyImg = Room2Panel.loadRoomImage("늑대_문장_열쇠");
            private Font titleFont = new Font("바탕", Font.BOLD, 22);
            private Font bodyFont = new Font("바탕", Font.BOLD, 16);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g2d.setColor(new Color(22, 18, 15));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // 1. 좌측: 기사 임명장(양피지) 이미지 및 임명 문구 출력
                int certX = 35, certY = 25, certW = 430, certH = 560;
                if (certImg != null) {
                    g2d.drawImage(certImg, certX, certY, certW, certH, this);
                }

                String[] certLines = {
                    "[ 왕실 기사 임명장 ]",
                    "",
                    "왕좌의 정면에서 고독하게",
                    "왕을 호위하는 '늑대'의 기사여,",
                    "",
                    "그대에게 왕실의 수호자 자격과",
                    "늑대 문장 열쇠를 하사하노라."
                };

                int startY = certY + 185;
                int lineHeight = 32;
                for (int i = 0; i < certLines.length; i++) {
                    String line = certLines[i];
                    if (line.isEmpty()) continue;

                    g2d.setFont(i == 0 ? titleFont : bodyFont);
                    FontMetrics fm = g2d.getFontMetrics();
                    int tx = certX + (certW - fm.stringWidth(line)) / 2;
                    int ty = startY + (i * lineHeight);

                    g2d.setColor(new Color(40, 25, 15));
                    g2d.drawString(line, tx, ty);
                }

                // 2. 우측: 늑대 문장 열쇠 이미지 및 설명 카드 출력
                int keyBoxX = 495, keyBoxY = 120, keyBoxW = 325, keyBoxH = 360;
                g2d.setColor(new Color(38, 30, 25));
                g2d.fillRoundRect(keyBoxX, keyBoxY, keyBoxW, keyBoxH, 20, 20);
                g2d.setColor(new Color(185, 145, 75));
                g2d.setStroke(new BasicStroke(2.5f));
                g2d.drawRoundRect(keyBoxX, keyBoxY, keyBoxW, keyBoxH, 20, 20);

                if (keyImg != null) {
                    g2d.drawImage(keyImg, keyBoxX + 20, keyBoxY + 30, keyBoxW - 40, 220, this);
                }

                g2d.setFont(new Font("바탕", Font.BOLD, 18));
                g2d.setColor(new Color(255, 225, 150));
                String keyTitle = "[ 늑대 문장 열쇠 ] 획득";
                FontMetrics fmKey = g2d.getFontMetrics();
                g2d.drawString(keyTitle, keyBoxX + (keyBoxW - fmKey.stringWidth(keyTitle)) / 2, keyBoxY + 295);

                g2d.setFont(new Font("바탕", Font.PLAIN, 14));
                g2d.setColor(new Color(220, 205, 180));
                String keyDesc = "늑대의 자리 아래에 숨겨져 있던 열쇠다.";
                FontMetrics fmDesc = g2d.getFontMetrics();
                g2d.drawString(keyDesc, keyBoxX + (keyBoxW - fmDesc.stringWidth(keyDesc)) / 2, keyBoxY + 328);

                g2d.dispose();
            }
        };

        add(panel);
    }
}