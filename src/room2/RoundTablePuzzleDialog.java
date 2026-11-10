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

public class RoundTablePuzzleDialog extends JDialog {
    private final Room2Panel parentRoom;

    // 하단 5개 기사 문장 패 (좌 -> 우): 0=사자, 1=늑대, 2=독수리, 3=곰, 4=사슴
    private final String[] knightNames = {"사자", "늑대", "독수리", "곰", "사슴"};

    // 원탁 주변 5개 빈 의자 방향 라벨 (시계 방향: 2시, 4시, 6시, 8시, 10시)
    private final String[] seatLabels = {"2시 의자", "4시 의자", "6시 의자", "8시 의자", "10시 의자"};

    // 정답 배치: 2시=독수리(2), 4시=곰(3), 6시=늑대(1), 8시=사슴(4), 10시=사자(0)
    private final int[] answerSeats = {2, 3, 1, 4, 0};

    // 현재 각 의자에 놓인 문장 번호 (-1이면 비어있음)
    private final int[] currentSeats = {-1, -1, -1, -1, -1};

    // 마우스 드래그 상태 변수
    private int draggingKnight = -1;
    private int draggingFromSeat = -1;
    private int dragX = 0;
    private int dragY = 0;
    private int hoverSeat = -1;

    private BufferedImage bgImg;
    private final BufferedImage[] shieldSprites = new BufferedImage[5];
    private String missingPathInfo = "";

    // 이미지 하단 나무 거치대 위 5개 황금 방패의 비율 좌표 (x, y, w, h)
    private final double[][] pieceRatios = {
        {0.125, 0.766, 0.094, 0.188}, // 0: 사자
        {0.291, 0.766, 0.094, 0.188}, // 1: 늑대
        {0.453, 0.766, 0.094, 0.188}, // 2: 독수리
        {0.617, 0.766, 0.094, 0.188}, // 3: 곰
        {0.781, 0.766, 0.094, 0.188}  // 4: 사슴
    };

    // 원탁 주변 5개 붉은 의자의 드롭 영역 비율 좌표 (x, y, w, h)
    private final double[][] seatRatios = {
        {0.652, 0.180, 0.088, 0.172}, // 0: 2시 의자 (우상단)
        {0.675, 0.390, 0.088, 0.172}, // 1: 4시 의자 (우하단)
        {0.456, 0.460, 0.088, 0.172}, // 2: 6시 의자 (중앙 하단 - 왕좌 맞은편)
        {0.235, 0.390, 0.088, 0.172}, // 3: 8시 의자 (좌하단)
        {0.265, 0.180, 0.088, 0.172}  // 4: 10시 의자 (좌상단)
    };

    public RoundTablePuzzleDialog(Window parent, Room2Panel parentRoom) {
        super(parent, "원탁의 빈자리", ModalityType.APPLICATION_MODAL);
        this.parentRoom = parentRoom;

        setSize(960, 540);
        setLocationRelativeTo(parent);
        setResizable(false);

        loadPuzzleSprites();

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
                    g2d.setColor(new Color(35, 25, 20));
                    g2d.fillRect(0, 0, w, h);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("맑은 고딕", Font.BOLD, 15));
                    g2d.drawString("이미지를 찾을 수 없습니다: " + missingPathInfo, 40, 80);
                }

                // 상단 안내 배너
                g2d.setColor(new Color(20, 14, 10, 180));
                g2d.fillRoundRect(w / 2 - 190, 14, 380, 32, 16, 16);
                g2d.setColor(new Color(215, 175, 85));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(w / 2 - 190, 14, 380, 32, 16, 16);
                drawLabel(g2d, "하단의 기사 문장 방패를 드래그하여 빈 의자에 배치하세요", w / 2, 35);

                // 1. 하단 5개 방패 거치대 처리 (이미 의자에 놓였거나 드래그 중인 방패는 빈 홈으로 표시)
                for (int i = 0; i < 5; i++) {
                    Rectangle r = getPieceRect(i, w, h);
                    if (isKnightPlaced(i) || draggingKnight == i) {
                        Polygon hole = createShieldPolygon(r.x, r.y, r.width, r.height);
                        g2d.setColor(new Color(22, 12, 8, 220));
                        g2d.fillPolygon(hole);
                        g2d.setColor(new Color(90, 55, 30, 170));
                        g2d.setStroke(new BasicStroke(2f));
                        g2d.drawPolygon(hole);
                    } else {
                        drawLabel(g2d, knightNames[i], r.x + r.width / 2, r.y + r.height + 13);
                    }
                }

                // 2. 원탁 주변 5개 의자 슬롯 및 장착된 방패 표시
                for (int s = 0; s < 5; s++) {
                    Rectangle sr = getSeatRect(s, w, h);
                    Polygon seatPoly = createShieldPolygon(sr.x, sr.y, sr.width, sr.height);

                    // 드래그 중일 때 놓을 수 있는 의자 위치에 금빛 가이드라인 표시
                    if (draggingKnight != -1) {
                        if (hoverSeat == s) {
                            g2d.setColor(new Color(255, 225, 90, 100));
                            g2d.fillPolygon(seatPoly);
                            g2d.setColor(new Color(255, 240, 130));
                            g2d.setStroke(new BasicStroke(3.2f));
                        } else {
                            g2d.setColor(new Color(255, 215, 80, 130));
                            g2d.setStroke(new BasicStroke(1.8f));
                        }
                        g2d.drawPolygon(seatPoly);
                    }

                    // 의자에 방패가 배치되어 있으면 방패 그리기
                    if (currentSeats[s] != -1) {
                        drawShieldPiece(g2d, currentSeats[s], sr.x, sr.y, sr.width, sr.height, false);
                    } else {
                        // 빈 의자일 때는 몇 시 방향 의자인지 라벨 표시
                        drawLabel(g2d, seatLabels[s], sr.x + sr.width / 2, sr.y + sr.height / 2 + 10);
                    }
                }

                // 3. 현재 마우스로 드래그 중인 방패 그리기 (마우스 포인터 중심)
                if (draggingKnight != -1) {
                    int dw = (int) (w * 0.096);
                    int dh = (int) (h * 0.192);
                    drawShieldPiece(g2d, draggingKnight, dragX - dw / 2, dragY - dh / 2, dw, dh, true);
                }

                g2d.dispose();
            }
        };
        puzzlePanel.setLayout(null);
        puzzlePanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 마우스 드래그 앤 드롭 이벤트 리스너
        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (parentRoom.hasWolfKey()) {
                    JOptionPane.showMessageDialog(
                        RoundTablePuzzleDialog.this,
                        "이미 원탁의 퍼즐을 해결하여 [기사 임명장]과 [늑대 문장 열쇠]를 획득했습니다."
                    );
                    return;
                }

                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                Point pt = e.getPoint();

                // 1) 의자에 이미 올려둔 방패를 다시 잡았을 때 -> 빼내서 이동
                for (int s = 0; s < 5; s++) {
                    if (currentSeats[s] != -1 && getSeatRect(s, w, h).contains(pt)) {
                        draggingKnight = currentSeats[s];
                        draggingFromSeat = s;
                        currentSeats[s] = -1;
                        dragX = pt.x;
                        dragY = pt.y;
                        hoverSeat = s;
                        puzzlePanel.repaint();
                        return;
                    }
                }

                // 2) 하단 거치대의 5개 방패 중 아직 안 쓰인 방패를 잡았을 때
                for (int i = 0; i < 5; i++) {
                    if (!isKnightPlaced(i) && getPieceRect(i, w, h).contains(pt)) {
                        draggingKnight = i;
                        draggingFromSeat = -1;
                        dragX = pt.x;
                        dragY = pt.y;
                        hoverSeat = -1;
                        puzzlePanel.repaint();
                        return;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (draggingKnight != -1) {
                    dragX = e.getX();
                    dragY = e.getY();
                    hoverSeat = findTargetSeat(e.getPoint(), puzzlePanel.getWidth(), puzzlePanel.getHeight());
                    puzzlePanel.repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingKnight == -1) {
                    return;
                }

                int w = puzzlePanel.getWidth();
                int h = puzzlePanel.getHeight();
                int targetSeat = findTargetSeat(e.getPoint(), w, h);

                if (targetSeat != -1) {
                    // 놓으려는 의자에 이미 다른 방패가 있고, 다른 의자에서 끌고 온 경우 서로 위치 교환(Swap)
                    if (currentSeats[targetSeat] != -1 && draggingFromSeat != -1) {
                        currentSeats[draggingFromSeat] = currentSeats[targetSeat];
                    }
                    currentSeats[targetSeat] = draggingKnight;
                }

                draggingKnight = -1;
                draggingFromSeat = -1;
                hoverSeat = -1;
                puzzlePanel.repaint();

                // 5개 의자가 모두 채워졌을 때 자동으로 정답 판정
                boolean allFilled = true;
                for (int s = 0; s < 5; s++) {
                    if (currentSeats[s] == -1) {
                        allFilled = false;
                        break;
                    }
                }
                if (allFilled) {
                    SwingUtilities.invokeLater(() -> checkAnswer(puzzlePanel));
                }
            }
        };

        puzzlePanel.addMouseListener(dragHandler);
        puzzlePanel.addMouseMotionListener(dragHandler);

        // 우측 상단에 [왕의 밀서 보기] 버튼 배치
        JButton viewLetterBtn = new JButton("왕의 밀서 보기");
        viewLetterBtn.setBounds(790, 14, 145, 34);
        viewLetterBtn.setFont(new Font("바탕", Font.BOLD, 13));
        viewLetterBtn.setBackground(new Color(90, 35, 25));
        viewLetterBtn.setForeground(new Color(255, 235, 190));
        viewLetterBtn.setFocusPainted(false);
        viewLetterBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewLetterBtn.addActionListener(e -> new RoyalSecretLetterDialog(this).setVisible(true));
        puzzlePanel.add(viewLetterBtn);

        add(puzzlePanel);
    }

    // 파일명에 '_2'가 붙은 경우와 기본 이름 모두 자동으로 탐색하여 로드 및 방패 크롭
    private void loadPuzzleSprites() {
        String[] candidates = {
            "resources/room2/원탁의_기사_자리배치_퍼즐_2.png",
            "resources/room2/원탁의_기사_자리배치_퍼즐.png",
            "resources/room2/원탁의 기사 자리배치 퍼즐 2.png",
            "resources/room2/원탁의 기사 자리배치 퍼즐.png"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                try {
                    bgImg = ImageIO.read(f);
                    if (bgImg != null) {
                        int imgW = bgImg.getWidth();
                        int imgH = bgImg.getHeight();
                        for (int i = 0; i < 5; i++) {
                            int sx = Math.max(0, (int) (imgW * pieceRatios[i][0]));
                            int sy = Math.max(0, (int) (imgH * pieceRatios[i][1]));
                            int sw = Math.min(imgW - sx, (int) (imgW * pieceRatios[i][2]));
                            int sh = Math.min(imgH - sy, (int) (imgH * pieceRatios[i][3]));
                            if (sw > 0 && sh > 0) {
                                shieldSprites[i] = bgImg.getSubimage(sx, sy, sw, sh);
                            }
                        }
                        return;
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        missingPathInfo = new File("resources/room2/원탁의_기사_자리배치_퍼즐.png").getAbsolutePath();
    }

    // 방패 외형(다각형 실루엣) 생성
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

    private void drawShieldPiece(Graphics2D g2d, int idx, int x, int y, int w, int h, boolean isDragging) {
        Polygon poly = createShieldPolygon(x, y, w, h);

        if (isDragging) {
            Polygon shadow = createShieldPolygon(x + 8, y + 12, w, h);
            g2d.setColor(new Color(0, 0, 0, 145));
            g2d.fillPolygon(shadow);
        }

        if (shieldSprites[idx] != null) {
            Shape oldClip = g2d.getClip();
            g2d.setClip(new Area(poly));
            g2d.drawImage(shieldSprites[idx], x, y, w, h, this);
            g2d.setClip(oldClip);
        } else {
            g2d.setColor(new Color(185, 145, 60));
            g2d.fillPolygon(poly);
        }

        g2d.setColor(isDragging ? new Color(255, 235, 130) : new Color(215, 170, 65));
        g2d.setStroke(new BasicStroke(isDragging ? 2.8f : 2.0f));
        g2d.drawPolygon(poly);

        drawLabel(g2d, knightNames[idx], x + w / 2, y + h + 15);
    }

    private void drawLabel(Graphics2D g2d, String text, int centerX, int y) {
        g2d.setFont(new Font("바탕", Font.BOLD, 13));
        FontMetrics fm = g2d.getFontMetrics();
        int tx = centerX - fm.stringWidth(text) / 2;
        g2d.setColor(new Color(0, 0, 0, 210));
        g2d.drawString(text, tx + 1, y + 1);
        g2d.setColor(new Color(255, 235, 165));
        g2d.drawString(text, tx, y);
    }

    private boolean isKnightPlaced(int knightIdx) {
        for (int s = 0; s < 5; s++) {
            if (currentSeats[s] == knightIdx) {
                return true;
            }
        }
        return false;
    }

    private int findTargetSeat(Point pt, int w, int h) {
        for (int s = 0; s < 5; s++) {
            Rectangle sr = getSeatRect(s, w, h);
            Rectangle magnet = new Rectangle(sr.x - 22, sr.y - 22, sr.width + 44, sr.height + 44);
            if (magnet.contains(pt)) {
                return s;
            }
        }
        return -1;
    }

    private Rectangle getPieceRect(int i, int w, int h) {
        return new Rectangle(
            (int) (w * pieceRatios[i][0]),
            (int) (h * pieceRatios[i][1]),
            (int) (w * pieceRatios[i][2]),
            (int) (h * pieceRatios[i][3])
        );
    }

    private Rectangle getSeatRect(int s, int w, int h) {
        return new Rectangle(
            (int) (w * seatRatios[s][0]),
            (int) (h * seatRatios[s][1]),
            (int) (w * seatRatios[s][2]),
            (int) (h * seatRatios[s][3])
        );
    }

    private void checkAnswer(JPanel puzzlePanel) {
        if (Arrays.equals(currentSeats, answerSeats)) {
            parentRoom.setKnightPuzzleSolved(true);

            JOptionPane.showMessageDialog(
                this,
                "철컥-! 다섯 기사의 문장 방패가 원탁의 각 자리에 맞물렸습니다.\n" +
                "왕좌와 마주 보는 6시 방향 [늑대]의 자리 아래에서\n" +
                "[기사 임명장]과 [늑대 문장 열쇠]를 획득했습니다!",
                "원탁의 빈자리 해결",
                JOptionPane.INFORMATION_MESSAGE
            );

            Window owner = getOwner();
            dispose();
            new KnightRewardDialog(owner).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(
                this,
                "문장 방패들이 서로 밀어내며 자리에서 튕겨 나왔습니다.\n[왕의 밀서]의 자리 조건을 다시 확인해 보세요.",
                "배치 실패",
                JOptionPane.WARNING_MESSAGE
            );

            Arrays.fill(currentSeats, -1);
            puzzlePanel.repaint();
        }
    }
}