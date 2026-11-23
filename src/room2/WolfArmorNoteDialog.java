package room2;

import javax.swing.*;
import java.awt.*;

public class WolfArmorNoteDialog extends JDialog {

    public WolfArmorNoteDialog(Window parent, Room2Panel parentRoom) {
        this(parent, parentRoom, true);
    }

    public WolfArmorNoteDialog(Window parent, Room2Panel parentRoom, boolean showGuide) {
        super(parent, "늑대 갑옷 속 쪽지", ModalityType.APPLICATION_MODAL);
        setSize(680, 480);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel panel = new JPanel() {
            private final Image noteImg = Room2Panel.loadRoomImage("갑옷_속_쪽지");
            private final Font titleFont = new Font("바탕", Font.BOLD, 18);
            private final Font bodyFont = new Font("바탕", Font.BOLD, 17);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                g2d.setColor(new Color(25, 20, 18));
                g2d.fillRect(0, 0, getWidth(), getHeight());

                if (noteImg != null) {
                    g2d.drawImage(noteImg, 0, 0, getWidth(), getHeight(), this);
                }

                String[] lines = {
                    "[ 늑대 갑옷 속 쪽지 ]",
                    "",
                    "“동쪽은 무기고,",
                    "그 맞은편은 막다른 길,",
                    "남쪽은 무너졌다.”"
                };

                int startY = 175;
                int lineHeight = 32;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    g2d.setFont(i == 0 ? titleFont : bodyFont);
                    FontMetrics fm = g2d.getFontMetrics();

                    int startX = (getWidth() - fm.stringWidth(line)) / 2;
                    int currentY = startY + (i * lineHeight);

                    g2d.setColor(new Color(90, 60, 35, 70));
                    g2d.drawString(line, startX + 1, currentY + 1);

                    g2d.setColor(new Color(40, 25, 15));
                    g2d.drawString(line, startX, currentY);
                }

                g2d.dispose();
            }
        };
        panel.setLayout(null);

        // 퍼즐 직접 진입 버튼은 제거하고 닫기 버튼만 배치
        JButton closeBtn = new JButton("쪽지 접기");
        closeBtn.setBounds(280, 405, 120, 34);
        closeBtn.setFont(new Font("바탕", Font.BOLD, 13));
        closeBtn.setBackground(new Color(75, 45, 25));
        closeBtn.setForeground(new Color(255, 235, 190));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> dispose());
        panel.add(closeBtn);

        add(panel);
    }
}