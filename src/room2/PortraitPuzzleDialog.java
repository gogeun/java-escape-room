package room2;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PortraitPuzzleDialog extends JDialog {
    // 초상화 배치(좌 -> 우): 1번(아렌), 2번(세른), 3번(다렌), 4번(베른)
    // 정답 순서: 아렌(1) -> 세른(2) -> 베른(4) -> 다렌(3)
    private final List<Integer> answerSequence = Arrays.asList(1, 2, 4, 3);
    private final List<Integer> currentInput = new ArrayList<>();
    private final JButton[] portraitButtons = new JButton[4];

    private final Room2Panel parentRoom;

    public PortraitPuzzleDialog(Window parent, Room2Panel parentRoom) {
        super(parent, "왕실 초상화 장식장", ModalityType.APPLICATION_MODAL);
        this.parentRoom = parentRoom;

        setSize(960, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel puzzlePanel = new JPanel() {
            private Image bgImg = new ImageIcon("resources/room2/왕_초상화_계승순서_퍼즐.png").getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth();
                int h = getHeight();

                if (bgImg != null) {
                    g2d.drawImage(bgImg, 0, 0, w, h, this);
                }

                String[] kingNames = {"아렌", "세른", "다렌", "베른"};
                double[] plateRatioX = {0.218, 0.408, 0.595, 0.783};
                int plateCenterY = (int) (h * 0.736); // 금색 명패 정중앙 높이

                g2d.setFont(new Font("바탕", Font.BOLD, 20));
                FontMetrics fm = g2d.getFontMetrics();

                for (int i = 0; i < 4; i++) {
                    String name = kingNames[i];
                    int textW = fm.stringWidth(name);
                    int drawX = (int) (w * plateRatioX[i]) - (textW / 2);
                    int drawY = plateCenterY + (fm.getAscent() - fm.getDescent()) / 2;

                    // 명패 각인 느낌을 주는 밝은 금빛 하이라이트
                    g2d.setColor(new Color(255, 230, 150, 160));
                    g2d.drawString(name, drawX + 1, drawY + 1);

                    // 진한 고동색 본문 텍스트 (금색 명패 위에서 선명하게 보임)
                    g2d.setColor(new Color(35, 18, 5));
                    g2d.drawString(name, drawX, drawY);
                }

                g2d.dispose();
            }
        };
        puzzlePanel.setLayout(null);

        // 4개의 초상화 액자 클릭 영역
        int[][] portraitBounds = {
            {150, 130, 135, 210}, // 1번 액자 (아렌)
            {325, 130, 135, 210}, // 2번 액자 (세른)
            {505, 130, 135, 210}, // 3번 액자 (다렌)
            {685, 130, 135, 210}  // 4번 액자 (베른)
        };

        String[] kingNames = {"아렌", "세른", "다렌", "베른"};

        for (int i = 0; i < 4; i++) {
            final int portraitNumber = i + 1;
            JButton btn = new JButton();
            btn.setBounds(
                portraitBounds[i][0],
                portraitBounds[i][1],
                portraitBounds[i][2],
                portraitBounds[i][3]
            );
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setToolTipText(kingNames[i] + "의 초상화"); // 마우스를 올렸을 때도 이름 표시

            btn.addActionListener(e -> handlePortraitClick(portraitNumber, btn));
            portraitButtons[i] = btn;
            puzzlePanel.add(btn);
        }

        add(puzzlePanel);
    }

    private void handlePortraitClick(int portraitNumber, JButton clickedBtn) {
        if (parentRoom.hasRoyalPass()) {
            JOptionPane.showMessageDialog(this, "이미 장치가 해제되어 왕실 인증패를 획득했습니다.");
            return;
        }

        if (currentInput.contains(portraitNumber)) {
            return;
        }

        currentInput.add(portraitNumber);
        clickedBtn.setBorderPainted(true);
        clickedBtn.setBorder(BorderFactory.createLineBorder(new Color(255, 215, 0), 3));

        if (currentInput.size() == 4) {
            checkAnswer();
        }
    }

    private void checkAnswer() {
        if (currentInput.equals(answerSequence)) {
            parentRoom.setRoyalPassAcquired(true);

            for (JButton btn : portraitButtons) {
                btn.setBorderPainted(false);
            }

            JOptionPane.showMessageDialog(
                this,
                "철컥- 소리와 함께 초상화 아래 비밀 벽감이 열렸습니다!\n[왕실 인증패]를 획득했습니다. 인증패 뒷면의 기록을 확인합니다.",
                "퍼즐 해결",
                JOptionPane.INFORMATION_MESSAGE
            );

            Window owner = getOwner();
            dispose();
            new RoyalPassDialog(owner).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "초상화 뒤에서 둔탁한 소리가 나며 장치가 초기화되었습니다.\n계승 순서가 틀린 것 같습니다.",
                "장치 작동 실패",
                JOptionPane.WARNING_MESSAGE
            );

            currentInput.clear();
            for (JButton btn : portraitButtons) {
                btn.setBorderPainted(false);
            }
        }
    }
}