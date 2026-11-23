package room2;

import javax.swing.*;
import java.awt.*;

public class WolfArmorNoteDialog extends JDialog {
    private final Image noteImg = Room2Panel.loadRoomImage("갑옷_속_쪽지");

    public WolfArmorNoteDialog(Window parent, Room2Panel parentRoom) {
        this(parent, parentRoom, true);
    }

    public WolfArmorNoteDialog(Window parent, Room2Panel parentRoom, boolean showOpenMapButton) {
        super(parent, "O10. 기사 갑옷 - 늑대 갑옷 속 쪽지", ModalityType.APPLICATION_MODAL);
        setSize(780, 520);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel() {
            private final Font titleFont = new Font("바탕", Font.BOLD, 24);
            private final Font subFont = new Font("바탕", Font.BOLD, 16);
            private final Font clueFont = new Font("바탕", Font.BOLD, 22);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                if (noteImg != null) {
                    g2d.drawImage(noteImg, 0, 0, w, h, this);
                }

                // 1. 상단 제목
                g2d.setFont(titleFont);
                drawInkText(g2d, "[ 늑대 갑옷 속 쪽지 ]", w / 2, 125, new Color(45, 28, 15));

                // 2. 부연 설명
                g2d.setFont(subFont);
                drawInkText(g2d, "늑대 문장 열쇠로 갑옷의 잠금장치를 열자 낡은 쪽지가 나타났다.", w / 2, 168, new Color(75, 50, 30));

                // 구분선
                g2d.setColor(new Color(110, 75, 45, 110));
                g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawLine(160, 192, w - 160, 192);

                // 3. 핵심 단서 문구
                g2d.setFont(clueFont);
                String[] clueLines = {
                    "“동쪽은 무기고,",
                    "그 맞은편은 막다른 길,",
                    "남쪽은 무너졌다.”"
                };

                int startY = 248;
                int lineHeight = 46;
                for (int i = 0; i < clueLines.length; i++) {
                    drawInkText(g2d, clueLines[i], w / 2, startY + (i * lineHeight), new Color(125, 22, 18));
                }

                g2d.dispose();
            }

            private void drawInkText(Graphics2D g2d, String text, int centerX, int y, Color mainColor) {
                FontMetrics fm = g2d.getFontMetrics();
                int x = centerX - (fm.stringWidth(text) / 2);

                g2d.setColor(new Color(90, 60, 35, 70));
                g2d.drawString(text, x + 1, y + 1);

                g2d.setColor(mainColor);
                g2d.drawString(text, x, y);
            }
        };
        panel.setLayout(null);

        if (showOpenMapButton && parentRoom != null) {
            JButton openMapBtn = new JButton("출구 안내도 펼치기 (문 선택)");
            openMapBtn.setBounds((780 - 250) / 2, 405, 250, 42);
            openMapBtn.setFont(new Font("바탕", Font.BOLD, 15));
            openMapBtn.setBackground(new Color(65, 40, 22));
            openMapBtn.setForeground(new Color(255, 235, 190));
            openMapBtn.setBorder(BorderFactory.createLineBorder(new Color(195, 155, 80), 2));
            openMapBtn.setFocusPainted(false);
            openMapBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            openMapBtn.addActionListener(e -> {
                Window owner = getOwner();
                dispose();
                new SecretExitPuzzleDialog(owner, parentRoom).setVisible(true);
            });
            panel.add(openMapBtn);
        } else {
            JButton closeBtn = new JButton("쪽지 접기");
            closeBtn.setBounds((780 - 140) / 2, 405, 140, 40);
            closeBtn.setFont(new Font("바탕", Font.BOLD, 15));
            closeBtn.setBackground(new Color(65, 40, 22));
            closeBtn.setForeground(new Color(255, 235, 190));
            closeBtn.setBorder(BorderFactory.createLineBorder(new Color(195, 155, 80), 2));
            closeBtn.setFocusPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addActionListener(e -> dispose());
            panel.add(closeBtn);
        }

        add(panel);
    }
}