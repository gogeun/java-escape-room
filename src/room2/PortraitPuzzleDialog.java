package room2;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortraitPuzzleDialog extends JDialog {
    private final Room2Panel parentRoom;
    private final Image bgImg = Room2Panel.loadRoomImage("왕_초상화_계승순서_퍼즐");

    private final String[] kingNames = {"아렌", "세른", "다렌", "베른"};

    private final List<Integer> answerSequence1 = Arrays.asList(1, 2, 4, 3);
    private final List<Integer> answerSequence2 = Arrays.asList(2, 4, 1, 3);
    private final List<Integer> currentInput = new ArrayList<>();
    private final JButton[] portraitButtons = new JButton[4];
    private final JPanel puzzlePanel;

    private final double[][] portraitRatios = {
        {0.165, 0.290, 0.140, 0.405},
        {0.338, 0.290, 0.140, 0.405},
        {0.523, 0.290, 0.140, 0.405},
        {0.695, 0.290, 0.140, 0.405}
    };

    private final double[] nameplateCenterX = {0.235, 0.408, 0.593, 0.765};
    private final double nameplateCenterY = 0.742;

    public PortraitPuzzleDialog(Window parent, Room2Panel parentRoom) {
        super(parent, "왕실 초상화 장식장", ModalityType.APPLICATION_MODAL);
        this.parentRoom = parentRoom;

        setSize(960, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        puzzlePanel = new JPanel() {
            private final Font nameFont = new Font("바탕", Font.BOLD, 15);

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                if (bgImg != null) {
                    g2d.drawImage(bgImg, 0, 0, w, h, this);
                } else {
                    g2d.setColor(new Color(30, 20, 15));
                    g2d.fillRect(0, 0, w, h);
                }

                int pushOffset = 3;
                for (int i = 0; i < 4; i++) {
                    int portraitNum = i + 1;
                    if (currentInput.contains(portraitNum) && bgImg != null) {
                        int centerX = (int) (w * nameplateCenterX[i]) + 2;
                        int innerW = (int) (w * 0.106);
                        int innerX = centerX - (innerW / 2);
                        int innerY = (int) (h * 0.322);
                        int innerH = (int) (h * 0.336);

                        Shape oldClip = g2d.getClip();
                        g2d.clipRect(innerX, innerY + pushOffset, innerW, innerH - pushOffset);
                        g2d.drawImage(bgImg, 0, pushOffset, w, h, this);
                        g2d.setClip(oldClip);

                        g2d.setColor(new Color(10, 5, 2, 220));
                        g2d.fillRect(innerX, innerY, innerW, pushOffset);

                        g2d.setColor(new Color(0, 0, 0, 110));
                        g2d.fillRect(innerX, innerY + pushOffset, 3, innerH - pushOffset);
                        g2d.fillRect(innerX + innerW - 3, innerY + pushOffset, 3, innerH - pushOffset);
                    }
                }

                g2d.setFont(nameFont);
                FontMetrics fm = g2d.getFontMetrics();
                int centerY = (int) (h * nameplateCenterY);

                for (int i = 0; i < 4; i++) {
                    String name = kingNames[i];
                    int textWidth = fm.stringWidth(name);
                    int centerX = (int) (w * nameplateCenterX[i]);

                    int textX = centerX - (textWidth / 2);
                    int textY = centerY + (fm.getAscent() - fm.getDescent()) / 2;

                    g2d.setColor(new Color(255, 235, 170, 160));
                    g2d.drawString(name, textX + 1, textY + 1);

                    g2d.setColor(new Color(35, 18, 10));
                    g2d.drawString(name, textX, textY);
                }

                g2d.dispose();
            }
        };
        puzzlePanel.setLayout(null);

        for (int i = 0; i < 4; i++) {
            final int portraitNumber = i + 1;
            JButton btn = new JButton();

            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

            btn.addActionListener(e -> handlePortraitClick(portraitNumber));
            portraitButtons[i] = btn;
            puzzlePanel.add(btn);
        }

        puzzlePanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int pw = puzzlePanel.getWidth();
                int ph = puzzlePanel.getHeight();
                for (int i = 0; i < 4; i++) {
                    int bx = (int) (pw * portraitRatios[i][0]);
                    int by = (int) (ph * portraitRatios[i][1]);
                    int bw = (int) (pw * portraitRatios[i][2]);
                    int bh = (int) (ph * portraitRatios[i][3]);
                    portraitButtons[i].setBounds(bx, by, bw, bh);
                }
            }
        });

        add(puzzlePanel);
    }

    private void handlePortraitClick(int portraitNumber) {
        if (parentRoom.hasRoyalPass()) {
            JOptionPane.showMessageDialog(this, "이미 초상화 장치가 해제되어 [왕실 인증패]를 획득했습니다.");
            return;
        }

        if (currentInput.contains(portraitNumber)) {
            return;
        }

        currentInput.add(portraitNumber);
        puzzlePanel.repaint();

        if (currentInput.size() == 4) {
            checkAnswer();
        }
    }

    private void checkAnswer() {
        if (currentInput.equals(answerSequence1) || currentInput.equals(answerSequence2)) {
            parentRoom.setRoyalPassAcquired(true);

            JOptionPane.showMessageDialog(
                this,
                "철컥- 소리와 함께 초상화 아래 비밀 벽감이 열렸습니다!\n[왕실 인증패]를 획득했습니다.",
                "퍼즐 해결",
                JOptionPane.INFORMATION_MESSAGE
            );

            Window owner = getOwner();
            dispose();
            new RoyalPassDialog(owner).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "초상화 뒤에서 둔탁한 소리가 나며 장치가 초기화되었습니다.\n기록의 계승 순서를 다시 확인해 보세요.",
                "장치 초기화",
                JOptionPane.WARNING_MESSAGE
            );

            currentInput.clear();
            puzzlePanel.repaint();
        }
    }
}