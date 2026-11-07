package room2;

import javax.swing.*;
import java.awt.*;

public class Room2Panel extends JPanel {
    private Image backgroundImage;

    public Room2Panel() {
        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        backgroundImage = new ImageIcon("resources/room2/room2_bg.png").getImage();

        JButton scrollButton = new JButton();
        scrollButton.setBounds(20, 300, 170, 260);
        scrollButton.setContentAreaFilled(false);
        scrollButton.setBorderPainted(false);
        scrollButton.setFocusPainted(false);
        scrollButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        scrollButton.addActionListener(e -> showScrollPopup());

        add(scrollButton);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            int newWidth = (int) (getWidth() * 1.06);
            int newHeight = (int) (getHeight() * 1.06);
            g.drawImage(backgroundImage, 0, 0, newWidth, newHeight, this);
        }
    }

    private void showScrollPopup() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parentWindow, "계승 기록 두루마리", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setSize(960, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel popupPanel = new JPanel() {
            private Image popupImg = new ImageIcon("resources/room2/scroll_popup.png").getImage();
            private Font textFont = new Font("바탕", Font.BOLD, 15);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();

                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                if (popupImg != null) {
                    int w = (int) (getWidth() * 1.05);
                    int h = (int) (getHeight() * 1.05);
                    g2d.drawImage(popupImg, 0, 0, w, h, this);
                }

                String[] lines = {
                    "[ 왕실 계승 기록 ]",
                    "",
                    "태초의 빛을 열었던",
                    "첫 왕은 세린이었노라.",
                    "그의 숨결이 멎은 직후 옥좌를",
                    "이어받은 이는 오직 베렌뿐이었도다.",
                    "마지막 옥새를 묻은 패왕 다렌이",
                    "즉위하기 직전, 지혜로운 군주 아렌이",
                    "그 사이의 연대를 지탱하였도다."
                };

                g2d.setFont(textFont);
                FontMetrics fm = g2d.getFontMetrics();
                int startY = 160;
                int lineHeight = 27;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    int textWidth = fm.stringWidth(line);
                    int startX = (getWidth() - textWidth) / 2 + 25;

                    g2d.setColor(new Color(90, 60, 35, 70));
                    g2d.drawString(line, startX + 1, startY + (i * lineHeight) + 1);

                    g2d.setColor(new Color(40, 25, 15));
                    g2d.drawString(line, startX, startY + (i * lineHeight));
                }

                g2d.dispose();
            }
        };

        dialog.add(popupPanel);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame testFrame = new JFrame("2번 방 - 왕실의 비밀실 테스트");
            testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            testFrame.add(new Room2Panel());
            testFrame.pack();
            testFrame.setLocationRelativeTo(null);
            testFrame.setVisible(true);
        });
    }
}