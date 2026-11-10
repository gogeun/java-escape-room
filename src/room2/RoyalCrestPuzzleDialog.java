package room2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RoyalCrestPuzzleDialog extends JDialog {
    private final Room2Panel parentRoom;

    // 6개 문장 조각 이름 (좌 -> 우)
    private final String[] crestNames = {"사자", "독수리", "왕관", "검", "장미", "방패"};

    // 정답 조합: 사자(0), 왕관(2), 장미(4)
    private final Set<Integer> answerSet = new HashSet<>(Arrays.asList(0, 2, 4));

    // 상단 3개 홈에 끼워진 조각 번호 (-1이면 비어있음)
    private final int[] slotPieces = {-1, -1, -1};

    // 드래그 상태 관리 변수
    private int draggingPiece = -1;
    private int draggingFromSlot = -1;
    private int dragX = 0;
    private int dragY = 0;
    private int hoverSlot = -1;

    private BufferedImage bgImg;
    private final BufferedImage[] pieceSprites = new BufferedImage[6];
    private String loadedPathInfo = "";

    // 원본 이미지 내 하단 6개 방패 조각의 비율 좌표 (x, y, w, h)
    private final double[][] pieceRatios = {
        {0.076, 0.700, 0.092, 0.195}, // 0: 사자
        {0.231, 0.700, 0.092, 0.195}, // 1: 독수리
        {0.380, 0.700, 0.092, 0.195}, // 2: 왕관
        {0.529, 0.700, 0.092, 0.195}, // 3: 검
        {0.679, 0.700, 0.092, 0.195}, // 4: 장미
        {0.832, 0.700, 0.092, 0.195}  // 5: 방패
    };

    // 상단 3개 빈 홈의 비율 좌표 (x, y, w, h)
    private final double[][] slotRatios = {
        {0.388, 0.175, 0.084, 0.165}, // 0: 좌상단 홈
        {0.530, 0.175, 0.084, 0.165}, // 1: 우상단 홈
        {0.457, 0.422, 0.086, 0.168}  // 2: 하단 중앙 홈
    };

    public RoyalCrestPuzzleDialog(Window parent, Room2Panel parentRoom) {
        super(parent, "문장 보관함 - 진짜 왕가의 문장", ModalityType.APPLICATION_MODAL);
        this.parentRoom = parentRoom;

        setSize(960, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        loadPuzzleImageAndSprites();

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

                if (bgImg != null) {
                    g2d.drawImage(bgImg, 0, 0, w, h, this);
                } else {
                    g2d.setColor(new Color(40, 30, 25));
                    g2d.fillRect(0, 0, w, h);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("맑은 고딕", Font.BOLD, 15));
                    g2d.drawString("이미지를 찾을 수 없습니다: " + loadedPathInfo, 40, 80);
                }

                // 중앙 금속 명패 안내 문구
                g2d.setFont(new Font("바탕", Font.BOLD, 13));
                g2d.setColor(new Color(45, 25, 10));
                String guide = "문장을 드래그하여 홈에 끼우세요";
                FontMetrics fmGuide = g2d.getFontMetrics();
                g2d.drawString(guide, (w - fmGuide.stringWidth(guide)) / 2, (int) (h * 0.372));

                // 1. 이미 홈에 끼워졌거나 드래그 중인 하단 조각 위치를 어둡게 처리
                for (int i = 0; i < 6; i++) {
                    Rectangle r = getPieceRect(i, w, h);
                    if (isPieceUsed(i) || draggingPiece == i) {
                        Polygon shieldHole = createShieldPolygon(r.x, r.y, r.width, r.height);
                        g2d.setColor(new Color(18, 12, 8, 210));
                        g2d.fillPolygon(shieldHole);
                        g2d.setColor(new Color(60, 40, 25, 150));
                        g2d.setStroke(new BasicStroke(2f));
                        g2d.drawPolygon(shieldHole);
                    } else if (bgImg == null) {
                        // 이미지가 없을 때도 테스트할 수 있도록 기본 방패 도형 그리기
                        drawShieldPiece(g2d, i, r.x, r.y, r.width, r.height, false);
                    }
                }

                // 2. 드래그 중일 때 상단 3개 홈 가이드 하이라이트 표시
                for (int s = 0; s < 3; s++) {
                    Rectangle sr = getSlotRect(s, w, h);
                    Polygon slotPoly = createShieldPolygon(sr.x, sr.y, sr.width, sr.height);

                    if (draggingPiece != -1 || bgImg == null) {
                        if (hoverSlot == s) {
                            g2d.setColor(new Color(255, 225, 90, 90));
                            g2d.fillPolygon(slotPoly);
                            g2d.setColor(new Color(255, 235, 120));
                            g2d.setStroke(new BasicStroke(3.5f));
                        } else {
                            g2d.setColor(new Color(255, 215, 80, 140));
                            g2d.setStroke(new BasicStroke(2f));
                        }
                        g2d.drawPolygon(slotPoly);
                    }
                }

                // 3. 상단 3개 홈에 장착된 문장 조각 그리기
                for (int s = 0; s < 3; s++) {
                    int pieceIdx = slotPieces[s];
                    if (pieceIdx != -1) {
                        Rectangle sr = getSlotRect(s, w, h);
                        drawShieldPiece(g2d, pieceIdx, sr.x, sr.y, sr.width, sr.height, false);
                    }
                }

                // 4. 현재 마우스로 드래그 중인 문장 조각 그리기
                if (draggingPiece != -1) {
                    int drawW = (int) (w * 0.095);
                    int drawH = (int) (h * 0.195);
                    int dx = dragX - (drawW / 2);
                    int dy = dragY - (drawH / 2);
                    drawShieldPiece(g2d, draggingPiece, dx, dy, drawW, drawH, true);
                }

                g2d.dispose();
            }
        };
        puzzlePanel.setLayout(null);
        puzzlePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (parentRoom.hasRoyalSecretLetter()) {
                    JOptionPane.showMessageDialog(
                        RoyalCrestPuzzleDialog.this,
                        "이미 문장 보관함이 열려 [왕의 밀서]를 획득했습니다."
                    );
                    return;
                }

                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                Point pt = e.getPoint();

                // 1) 상단 홈에 끼워진 조각 빼내기
                for (int s = 0; s < 3; s++) {
                    if (slotPieces[s] != -1 && getSlotRect(s, w, h).contains(pt)) {
                        draggingPiece = slotPieces[s];
                        draggingFromSlot = s;
                        slotPieces[s] = -1;
                        dragX = pt.x;
                        dragY = pt.y;
                        hoverSlot = s;
                        puzzlePanel.repaint();
                        return;
                    }
                }

                // 2) 하단 테이블의 6개 문장 조각 잡기
                for (int i = 0; i < 6; i++) {
                    if (!isPieceUsed(i) && getPieceRect(i, w, h).contains(pt)) {
                        draggingPiece = i;
                        draggingFromSlot = -1;
                        dragX = pt.x;
                        dragY = pt.y;
                        hoverSlot = -1;
                        puzzlePanel.repaint();
                        return;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingPiece != -1) {
                    dragX = e.getX();
                    dragY = e.getY();
                    hoverSlot = findTargetSlot(e.getPoint(), puzzlePanel.getWidth(), puzzlePanel.getHeight());
                    puzzlePanel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingPiece == -1) {
                    return;
                }

                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                int targetSlot = findTargetSlot(e.getPoint(), w, h);

                if (targetSlot != -1) {
                    if (slotPieces[targetSlot] != -1 && draggingFromSlot != -1) {
                        slotPieces[draggingFromSlot] = slotPieces[targetSlot];
                    }
                    slotPieces[targetSlot] = draggingPiece;
                }

                draggingPiece = -1;
                draggingFromSlot = -1;
                hoverSlot = -1;
                puzzlePanel.repaint();

                if (slotPieces[0] != -1 && slotPieces[1] != -1 && slotPieces[2] != -1) {
                    SwingUtilities.invokeLater(() -> checkAnswer(puzzlePanel));
                }
            }
        };

        puzzlePanel.addMouseListener(dragHandler);
        puzzlePanel.addMouseMotionListener(dragHandler);

        JButton viewPassBtn = new JButton("인증패 뒷면 보기");
        viewPassBtn.setBounds(790, 20, 145, 35);
        viewPassBtn.setFont(new Font("바탕", Font.BOLD, 13));
        viewPassBtn.setBackground(new Color(70, 45, 25));
        viewPassBtn.setForeground(new Color(245, 225, 180));
        viewPassBtn.setFocusPainted(false);
        viewPassBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewPassBtn.addActionListener(e -> new RoyalPassDialog(this).setVisible(true));
        puzzlePanel.add(viewPassBtn);

        add(puzzlePanel);
    }

    private int findTargetSlot(Point pt, int panelW, int panelH) {
        for (int s = 0; s < 3; s++) {
            Rectangle sr = getSlotRect(s, panelW, panelH);
            Rectangle magnetZone = new Rectangle(sr.x - 22, sr.y - 22, sr.width + 44, sr.height + 44);
            if (magnetZone.contains(pt)) {
                return s;
            }
        }
        return -1;
    }

    private Polygon createShieldPolygon(int x, int y, int w, int h) {
        int[] px = {
            x + w / 2,
            x + w,
            x + (int) (w * 0.86),
            x + w / 2,
            x + (int) (w * 0.14),
            x
        };
        int[] py = {
            y,
            y + (int) (h * 0.10),
            y + (int) (h * 0.68),
            y + h,
            y + (int) (h * 0.68),
            y + (int) (h * 0.10)
        };
        return new Polygon(px, py, 6);
    }

    private void drawShieldPiece(Graphics2D g2d, int pieceIdx, int x, int y, int w, int h, boolean isDragging) {
        Polygon shieldPoly = createShieldPolygon(x, y, w, h);

        if (isDragging) {
            Polygon shadowPoly = createShieldPolygon(x + 8, y + 12, w, h);
            g2d.setColor(new Color(0, 0, 0, 140));
            g2d.fillPolygon(shadowPoly);
        }

        if (pieceSprites[pieceIdx] != null) {
            Shape oldClip = g2d.getClip();
            g2d.setClip(new Area(shieldPoly));
            g2d.drawImage(pieceSprites[pieceIdx], x, y, w, h, this);
            g2d.setClip(oldClip);
        } else {
            g2d.setColor(new Color(180, 140, 55));
            g2d.fillPolygon(shieldPoly);
        }

        g2d.setColor(isDragging ? new Color(255, 235, 130) : new Color(210, 165, 60));
        g2d.setStroke(new BasicStroke(isDragging ? 2.8f : 2.0f));
        g2d.drawPolygon(shieldPoly);

        g2d.setFont(new Font("바탕", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();
        String name = crestNames[pieceIdx];
        int textX = x + (w - fm.stringWidth(name)) / 2;
        int textY = y + h + (isDragging ? 16 : 14);

        g2d.setColor(new Color(0, 0, 0, 200));
        g2d.drawString(name, textX + 1, textY + 1);
        g2d.setColor(new Color(255, 235, 150));
        g2d.drawString(name, textX, textY);
    }

    // [핵심 수정] 이미지 크롭 시 경계를 벗어나지 않도록 안전장치 추가 및 다양한 방식의 로드 지원
    private void loadPuzzleImageAndSprites() {
        String[] candidatePaths = {
            "resources/room2/왕가의_문장_퍼즐.png",
            "resources/room2/왕가의 문장 퍼즐.png",
            "resources/room2/왕가의_문장_퍼즐.jpg",
            "resources/room2/왕가의 문장 퍼즐.jpg"
        };

        for (String path : candidatePaths) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    bgImg = ImageIO.read(file);
                    if (bgImg != null) {
                        int imgW = bgImg.getWidth();
                        int imgH = bgImg.getHeight();
                        for (int i = 0; i < 6; i++) {
                            int sx = Math.max(0, (int) (imgW * pieceRatios[i][0]));
                            int sy = Math.max(0, (int) (imgH * pieceRatios[i][1]));
                            int sw = Math.min(imgW - sx, (int) (imgW * pieceRatios[i][2]));
                            int sh = Math.min(imgH - sy, (int) (imgH * pieceRatios[i][3]));
                            if (sw > 0 && sh > 0) {
                                pieceSprites[i] = bgImg.getSubimage(sx, sy, sw, sh);
                            }
                        }
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        loadedPathInfo = new File("resources/room2/왕가의_문장_퍼즐.png").getAbsolutePath();
    }

    private boolean isPieceUsed(int pieceIdx) {
        for (int s = 0; s < 3; s++) {
            if (slotPieces[s] == pieceIdx) {
                return true;
            }
        }
        return false;
    }

    private Rectangle getPieceRect(int i, int panelW, int panelH) {
        int x = (int) (panelW * pieceRatios[i][0]);
        int y = (int) (panelH * pieceRatios[i][1]);
        int w = (int) (panelW * pieceRatios[i][2]);
        int h = (int) (panelH * pieceRatios[i][3]);
        return new Rectangle(x, y, w, h);
    }

    private Rectangle getSlotRect(int s, int panelW, int panelH) {
        int x = (int) (panelW * slotRatios[s][0]);
        int y = (int) (panelH * slotRatios[s][1]);
        int w = (int) (panelW * slotRatios[s][2]);
        int h = (int) (panelH * slotRatios[s][3]);
        return new Rectangle(x, y, w, h);
    }

    private void checkAnswer(JPanel puzzlePanel) {
        Set<Integer> playerSet = new HashSet<>();
        for (int piece : slotPieces) {
            playerSet.add(piece);
        }

        if (playerSet.equals(answerSet)) {
            parentRoom.setRoyalSecretLetterAcquired(true);

            JOptionPane.showMessageDialog(
                this,
                "철컥! 사자, 왕관, 장미 문장이 홈에 정확히 맞물리며 보관함이 열렸습니다!\n보관함 안에서 [왕의 밀서]를 획득했습니다. 밀서의 내용을 확인합니다.",
                "진짜 왕가의 문장 해결",
                JOptionPane.INFORMATION_MESSAGE
            );

            Window owner = getOwner();
            dispose();
            new RoyalSecretLetterDialog(owner).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "문장 조각들이 서로 밀어내며 홈에서 튕겨 나왔습니다.\n인증패 뒷면의 기록을 다시 확인해 보세요.",
                "조합 실패",
                JOptionPane.WARNING_MESSAGE
            );

            Arrays.fill(slotPieces, -1);
            puzzlePanel.repaint();
        }
    }
}