package room2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class SecretExitPuzzleDialog extends JDialog {
    private final Room2Panel parentRoom;
    private final Image mapBgImg = Room2Panel.loadRoomImage("비밀출구_지도_퍼즐");

    private final String[] doorNames = {"북쪽 문", "남쪽 문", "서쪽 문", "동쪽 문"};

    // 4개 아치형 나무 문 비율 좌표 (x, y, w, h)
    private final double[][] doorRatios = {
        {0.453, 0.095, 0.094, 0.190}, // 0: 북쪽 문 (상단 중앙)
        {0.453, 0.645, 0.094, 0.190}, // 1: 남쪽 문 (하단 중앙)
        {0.270, 0.365, 0.092, 0.190}, // 2: 서쪽 문 (좌측)
        {0.638, 0.365, 0.092, 0.190}  // 3: 동쪽 문 (우측)
    };

    private int hoverDoorIndex = -1;

    public SecretExitPuzzleDialog(Window parent, Room2Panel parentRoom) {
        super(parent, "O11. 출구 안내도 - 기사의 비밀 출구", ModalityType.APPLICATION_MODAL);
        this.parentRoom = parentRoom;

        setSize(960, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        JPanel puzzlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int w = getWidth();
                int h = getHeight();

                if (mapBgImg != null) {
                    g2d.drawImage(mapBgImg, 0, 0, w, h, this);
                }

                // 4개의 아치형 문 위치에 방향 라벨 및 마우스 호버 하이라이트 표시
                for (int i = 0; i < 4; i++) {
                    Rectangle r = getDoorRect(i, w, h);
                    boolean isHover = (hoverDoorIndex == i);

                    if (isHover) {
                        RoundRectangle2D archShape = new RoundRectangle2D.Double(
                            r.x, r.y, r.width, r.height, r.width * 0.85, r.width * 0.85
                        );
                        g2d.setColor(new Color(255, 220, 90, 75));
                        g2d.fill(archShape);
                        g2d.setColor(new Color(255, 235, 120));
                        g2d.setStroke(new BasicStroke(3.2f));
                        g2d.draw(archShape);
                    }

                    drawDoorLabel(g2d, doorNames[i], r.x + r.width / 2, r.y + r.height + 18, isHover);
                }

                g2d.dispose();
            }
        };
        puzzlePanel.setLayout(null);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                Point pt = e.getPoint();

                int found = -1;
                for (int i = 0; i < 4; i++) {
                    if (getDoorRect(i, w, h).contains(pt)) {
                        found = i;
                        break;
                    }
                }

                if (found != hoverDoorIndex) {
                    hoverDoorIndex = found;
                    puzzlePanel.setCursor(new Cursor(found != -1 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
                    puzzlePanel.repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                Point pt = e.getPoint();

                for (int i = 0; i < 4; i++) {
                    if (getDoorRect(i, w, h).contains(pt)) {
                        handleDoorSelection(doorNames[i]);
                        return;
                    }
                }
            }
        };

        puzzlePanel.addMouseListener(mouseHandler);
        puzzlePanel.addMouseMotionListener(mouseHandler);

        JButton noteBtn = new JButton("갑옷 속 쪽지 보기");
        noteBtn.setBounds(790, 16, 145, 34);
        noteBtn.setFont(new Font("바탕", Font.BOLD, 13));
        noteBtn.setBackground(new Color(75, 45, 25));
        noteBtn.setForeground(new Color(255, 235, 190));
        noteBtn.setFocusPainted(false);
        noteBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        noteBtn.addActionListener(e -> new WolfArmorNoteDialog(this, parentRoom, false).setVisible(true));
        puzzlePanel.add(noteBtn);

        add(puzzlePanel);
    }

    private Rectangle getDoorRect(int index, int panelW, int panelH) {
        int x = (int) (panelW * doorRatios[index][0]);
        int y = (int) (panelH * doorRatios[index][1]);
        int w = (int) (panelW * doorRatios[index][2]);
        int h = (int) (panelH * doorRatios[index][3]);
        return new Rectangle(x, y, w, h);
    }

    private void drawDoorLabel(Graphics2D g2d, String text, int centerX, int y, boolean isHover) {
        g2d.setFont(new Font("바탕", Font.BOLD, isHover ? 16 : 14));
        FontMetrics fm = g2d.getFontMetrics();
        int textW = fm.stringWidth(text);
        int boxW = textW + 18;
        int boxH = 24;
        int boxX = centerX - boxW / 2;
        int boxY = y - 17;

        g2d.setColor(isHover ? new Color(65, 38, 18, 230) : new Color(25, 18, 12, 185));
        g2d.fillRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        g2d.setColor(isHover ? new Color(255, 225, 100) : new Color(190, 150, 80, 180));
        g2d.setStroke(new BasicStroke(isHover ? 2f : 1.2f));
        g2d.drawRoundRect(boxX, boxY, boxW, boxH, 10, 10);

        g2d.setColor(isHover ? new Color(255, 245, 190) : new Color(240, 220, 175));
        g2d.drawString(text, centerX - textW / 2, y);
    }

    private void handleDoorSelection(String selectedDoor) {
        if ("북쪽 문".equals(selectedDoor)) {
            parentRoom.setSecretExitOpened(true);

            JOptionPane.showMessageDialog(
                this,
                "철컥- 쿠구궁!\n" +
                "[기사 임명장]과 [늑대 문장 열쇠]를 사용해 북쪽 비밀 출구를 열었습니다!\n" +
                "열린 북쪽 통로를 따라 3번 방으로 이동합니다.",
                "기사의 비밀 출구 개방 (정답)",
                JOptionPane.INFORMATION_MESSAGE
            );

            dispose();
            parentRoom.moveToRoom3();
        } else {
            String failReason;
            if ("동쪽 문".equals(selectedDoor)) {
                failReason = "동쪽 문 너머는 무기고라 밖으로 나갈 수 없습니다.";
            } else if ("서쪽 문".equals(selectedDoor)) {
                failReason = "동쪽의 맞은편인 서쪽 문 너머는 막다른 길입니다.";
            } else {
                failReason = "남쪽 문 너머 통로는 무너져 내려 지나갈 수 없습니다.";
            }

            JOptionPane.showMessageDialog(
                this,
                failReason + "\n늑대 갑옷 속 쪽지의 단서를 다시 확인해 보세요.",
                "잘못된 출구 선택",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }
}