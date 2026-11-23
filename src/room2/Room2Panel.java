package room2;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class Room2Panel extends JPanel {
    private Image backgroundImage;

    private boolean hasRoyalPass = false;          // I07 왕실 인증패 보유 여부
    private boolean hasRoyalSecretLetter = false;  // I08 왕의 밀서 보유 여부
    private boolean hasKnightCertificate = false;  // I09 기사 임명장 보유 여부
    private boolean hasWolfKey = false;            // I10 늑대 문장 열쇠 보유 여부
    private boolean isArmorNoteRead = false;       // O10 늑대 갑옷 속 쪽지 확인 여부
    private boolean isSecretExitOpened = false;    // 북쪽 비밀 출구 개방 여부

    // --- [하단 8칸 정사각형 인벤토리 관련 필드] ---
    private static final int INV_COLS = 8;
    private static final int INV_GAP = 12;
    private static final int INV_MARGIN_X = 24;
    private static final int INV_SLOT_SIZE = (1280 - (INV_MARGIN_X * 2) - (INV_GAP * (INV_COLS - 1))) / INV_COLS; // 143px
    private static final int INV_Y = 720 - INV_SLOT_SIZE - 15; // 562px

    private final JButton[] inventorySlots = new JButton[INV_COLS];
    private final String[] inventoryItemIds = new String[INV_COLS];

    // --- [우측 상단 음소거 버튼 관련 필드] ---
    private JButton soundButton;
    private boolean muted = Boolean.getBoolean("skyborne.mute");

    private Runnable onMoveToRoom3Callback;

    public Room2Panel() {
        this(null);
    }

    public Room2Panel(Runnable onMoveToRoom3Callback) {
        this.onMoveToRoom3Callback = onMoveToRoom3Callback;

        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        backgroundImage = loadRoomImage("중세방_전체배경");

        initScrollClueButton();      // 1. 좌측 하단 독서대 단서 열람
        initPortraitCabinetButton(); // 2. 좌측 벽면 초상화 장식장 (단순 클릭 -> 왕실 인증패 즉시 획득)
        initCrestBoxButton();        // 3. [퍼즐 1] P06 중앙 좌측 벽면 문장 보관함(방패) 퍼즐
        initRoundTableButton();      // 4. [퍼즐 2] P07 중앙 원탁의 빈자리 퍼즐
        initKnightArmorButton();     // 5. 우측 기사 갑옷 (단순 클릭 -> 갑옷 속 쪽지 즉시 획득)
        initExitMapButton();         // 6. 우측 벽면 출구 안내도 (단순 클릭 -> 비밀 출구 개방 및 3번 방 이동)
        initSoundButton();           // 7. 우측 상단 음소거 버튼
        initInventoryBar();          // 8. 하단 1줄 8칸 정사각형 인벤토리 바
    }

    // .png 및 기타 확장자 지원
    public static Image loadRoomImage(String baseName) {
        String[] exts = {".png", ".jpg", ".jpeg"};
        for (String ext : exts) {
            File f = new File("resources/room2/" + baseName + ext);
            if (f.exists()) {
                try {
                    return ImageIO.read(f);
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

private void initScrollClueButton() {
        JButton scrollButton = new JButton();
        scrollButton.setBounds(55, 305, 145, 75);
        makeMagnifierButton(scrollButton);

        scrollButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new ScrollPopupDialog(parentWindow).setVisible(true);
        });

        add(scrollButton);
    }

    // 2. 좌측 벽면 초상화 장식장 클릭 영역 (P05 퍼즐 제거 -> 클릭 시 즉시 왕실 인증패 획득)
    private void initPortraitCabinetButton() {
        JButton portraitCabinetBtn = new JButton();
        portraitCabinetBtn.setBounds(160, 150, 175, 125);
        makeMagnifierButton(portraitCabinetBtn);
        portraitCabinetBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            if (!hasRoyalPass) {
                setRoyalPassAcquired(true);
                JOptionPane.showMessageDialog(
                    this,
                    "역대 왕들의 초상화 장식장 아래 비밀 홈에서\n[왕실 인증패]를 발견했습니다!",
                    "단서 획득",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            new RoyalPassDialog(parentWindow).setVisible(true);
        });

        add(portraitCabinetBtn);
    }

    // 3. 벽면 붉은 방패 모양 '문장 보관함(O08)' 클릭 영역 ([퍼즐 1] P06)
    private void initCrestBoxButton() {
        JButton crestBoxBtn = new JButton();
        crestBoxBtn.setBounds(340, 35, 210, 245);
        makeMagnifierButton(crestBoxBtn);

        crestBoxBtn.addActionListener(e -> {
            if (!hasRoyalPass) {
                JOptionPane.showMessageDialog(
                    this,
                    "세 개의 홈이 파인 왕가의 문장 보관함이다.\n보관함을 조사하려면 먼저 [왕실 인증패]가 필요할 것 같다.",
                    "문장 보관함",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new RoyalCrestPuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(crestBoxBtn);
    }

    // 4. 방 중앙 '원탁과 의자들(O09)' 클릭 영역 ([퍼즐 2] P07)
    private void initRoundTableButton() {
        JButton roundTableBtn = new JButton();
        roundTableBtn.setBounds(350, 300, 540, 230);
        makeMagnifierButton(roundTableBtn);

        roundTableBtn.addActionListener(e -> {
            if (!hasRoyalSecretLetter) {
                JOptionPane.showMessageDialog(
                    this,
                    "왕좌 앞에 놓인 원탁과 다섯 개의 빈 의자다.\n자리 배치의 비밀이 적힌 [왕의 밀서]를 먼저 찾아야 할 것 같다.",
                    "원탁",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new RoundTablePuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(roundTableBtn);
    }

    private void initKnightArmorButton() {
        JButton armorBtn = new JButton();
        armorBtn.setBounds(800, 175, 255, 195);
        makeMagnifierButton(armorBtn);

        armorBtn.addActionListener(e -> {
            if (!hasKnightCertificate || !hasWolfKey) {
                JOptionPane.showMessageDialog(
                    this,
                    "복도에 줄지어 서 있는 기사 갑옷들이다.\n" +
                    "가슴에 늑대 문장 열쇠구멍이 있는 갑옷을 열려면\n" +
                    "먼저 원탁의 퍼즐을 풀어 [기사 임명장]과 [늑대 문장 열쇠]를 찾아야 한다.",
                    "기사 갑옷 (O10)",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            if (!isArmorNoteRead) {
                isArmorNoteRead = true;
                addItemToInventory("ARMOR_NOTE", "갑옷 속 쪽지", "갑옷_속_쪽지");
                JOptionPane.showMessageDialog(
                    this,
                    "[늑대 문장 열쇠]로 갑옷의 가슴판을 열었습니다.\n안쪽에서 [갑옷 속 쪽지]를 발견했습니다!",
                    "단서 획득",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new WolfArmorNoteDialog(parentWindow, this).setVisible(true);
        });

        add(armorBtn);
    }

    private void initExitMapButton() {
        JButton exitMapBtn = new JButton();
        exitMapBtn.setBounds(1070, 80, 175, 230);
        makeMagnifierButton(exitMapBtn);

        exitMapBtn.addActionListener(e -> {
            if (!hasKnightCertificate || !hasWolfKey) {
                JOptionPane.showMessageDialog(
                    this,
                    "동·서·남·북 네 방향의 통로가 그려진 출구 안내도다.\n" +
                    "비밀 통로를 열려면 먼저 [기사 임명장]과 [늑대 문장 열쇠]가 필요하다.",
                    "출구 안내도 (O11)",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            if (!isArmorNoteRead) {
                JOptionPane.showMessageDialog(
                    this,
                    "출구 안내도 옆에 늑대 문장이 새겨진 잠금장치가 있다.\n" +
                    "먼저 우측의 [기사 갑옷]을 조사해 단서를 확인해야 할 것 같다.",
                    "출구 안내도 (O11)",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new SecretExitPuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(exitMapBtn);
    }


    private void makeMagnifierButton(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics graphics, JComponent c) {
                AbstractButton b = (AbstractButton) c;
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int cx = c.getWidth() / 2;
                int cy = c.getHeight() / 2;
                boolean hover = b.getModel().isRollover();

                int r = 15; // 원형 배지 반지름 (지름 30px)

                // 1. 원형 배경 배지 (호버 시 밝은 브라스 톤, 평상시 반투명 다크 톤)
                g.setColor(hover ? new Color(92, 84, 74, 230) : new Color(36, 32, 27, 195));
                g.fillOval(cx - r, cy - r, r * 2, r * 2);

                // 2. 호버 시 황금빛 외곽 테두리 (image_218aab 스타일)
                if (hover) {
                    g.setColor(new Color(223, 185, 116));
                    g.setStroke(new BasicStroke(2.2f));
                    g.drawOval(cx - r + 1, cy - r + 1, (r - 1) * 2, (r - 1) * 2);
                } else {
                    g.setColor(new Color(142, 111, 70, 140));
                    g.setStroke(new BasicStroke(1.2f));
                    g.drawOval(cx - r, cy - r, r * 2, r * 2);
                }

                // 3. 중앙 돋보기 렌즈 + 손잡이 (drawOval & drawLine)
                g.setColor(hover ? new Color(223, 185, 116) : new Color(245, 230, 200));
                g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawOval(cx - 6, cy - 6, 9, 9);          // 돋보기 렌즈
                g.drawLine(cx + 2, cy + 2, cx + 7, cy + 7); // 돋보기 손잡이

                g.dispose();
            }
        });
    }

    public boolean hasRoyalPass() {
        return hasRoyalPass;
    }

    public void setRoyalPassAcquired(boolean acquired) {
        this.hasRoyalPass = acquired;
        if (acquired) {
            addItemToInventory("ROYAL_PASS", "왕실 인증패", "왕실_인증패");
        }
    }

    public boolean hasRoyalSecretLetter() {
        return hasRoyalSecretLetter;
    }

    public void setRoyalSecretLetterAcquired(boolean acquired) {
        this.hasRoyalSecretLetter = acquired;
        if (acquired) {
            addItemToInventory("ROYAL_LETTER", "왕의 밀서", "왕의_밀서");
        }
    }

    public boolean hasKnightCertificate() {
        return hasKnightCertificate;
    }

    public boolean hasWolfKey() {
        return hasWolfKey;
    }

    public void setKnightPuzzleSolved(boolean solved) {
        this.hasKnightCertificate = solved;
        this.hasWolfKey = solved;
        if (solved) {
            addItemToInventory("KNIGHT_CERT", "기사 임명장", "기사_임명장");
            addItemToInventory("WOLF_KEY", "늑대 문장 열쇠", "늑대_문장_열쇠");
        }
    }

    public boolean isSecretExitOpened() {
        return isSecretExitOpened;
    }

    public void setSecretExitOpened(boolean opened) {
        this.isSecretExitOpened = opened;
    }

    // 3번 방으로 이동 처리
    public void moveToRoom3() {
        if (onMoveToRoom3Callback != null) {
            onMoveToRoom3Callback.run();
        } else {
            JOptionPane.showMessageDialog(
                this,
                "축하합니다! 2번 방(왕실 계승의 방)의 모든 퍼즐을 풀고 [3번 방]으로 진입했습니다!",
                "2번 방 탈출 성공",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    // 7. 우측 상단 음소거 버튼
    private void initSoundButton() {
        soundButton = createBrassButton(muted ? "소리 꺼짐" : "소리 켜짐", this::toggleSound);
        updateSoundButtonBounds(1280);
        add(soundButton);
    }

    private void toggleSound() {
        muted = !muted;
        System.setProperty("skyborne.mute", String.valueOf(muted));
        soundButton.setText(muted ? "소리 꺼짐" : "소리 켜짐");
        updateSoundButtonBounds(getWidth() > 0 ? getWidth() : 1280);
    }

    private void updateSoundButtonBounds(int panelWidth) {
        Dimension size = soundButton.getPreferredSize();
        int width = size.width + 16;
        soundButton.setBounds(panelWidth - 25 - width, 15, width, size.height);
    }

    private JButton createBrassButton(String text, Runnable action) {
        final Color CREAM = new Color(245, 230, 200);
        final Color GOLD = new Color(223, 185, 116);
        final Color EDGE = new Color(142, 111, 70);

        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                boolean lit = getModel().isRollover() || isFocusOwner();
                Color top = lit ? new Color(105, 79, 48) : new Color(69, 55, 40);
                Color bottom = new Color(42, 33, 26);
                if (getModel().isArmed()) top = bottom;

                g.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bottom));
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
                g.setColor(lit ? GOLD : EDGE);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 7, 7);
                g.dispose();
                super.paintComponent(graphics);
            }
        };

        Font korean = new Font("Malgun Gothic", Font.BOLD, 14);
        if (!korean.getFamily().equals(Font.DIALOG) && korean.canDisplay('한')) {
            b.setFont(korean);
        } else {
            b.setFont(UIManager.getFont("Label.font").deriveFont(Font.BOLD, 14f));
        }

        b.setMargin(new Insets(0, 0, 0, 0));
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        b.setForeground(CREAM);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    // 8. 하단 1줄 8칸 정사각형 인벤토리 초기화
    private void initInventoryBar() {
        for (int i = 0; i < INV_COLS; i++) {
            final int slotIndex = i;
            int x = INV_MARGIN_X + i * (INV_SLOT_SIZE + INV_GAP);

            JButton slot = new JButton() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    boolean hasItem = inventoryItemIds[slotIndex] != null;
                    boolean hover = getModel().isRollover() && hasItem;

                    g.setColor(hasItem ? new Color(36, 28, 22, 235) : new Color(20, 17, 14, 195));
                    g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    g.setColor(hover ? new Color(235, 198, 125) : new Color(142, 111, 70, hasItem ? 255 : 150));
                    g.setStroke(new BasicStroke(hover ? 2.2f : 1.5f));
                    g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    g.setColor(new Color(90, 70, 45, 110));
                    g.setStroke(new BasicStroke(1f));
                    g.drawRoundRect(4, 4, getWidth() - 9, getHeight() - 9, 7, 7);

                    g.dispose();
                    super.paintComponent(graphics);
                }
            };

            slot.setBounds(x, INV_Y, INV_SLOT_SIZE, INV_SLOT_SIZE);
            slot.setContentAreaFilled(false);
            slot.setFocusPainted(false);
            slot.setBorderPainted(false);
            slot.setMargin(new Insets(6, 6, 6, 6));
            slot.setFont(new Font("Malgun Gothic", Font.BOLD, 14));
            slot.setForeground(new Color(245, 230, 200));
            slot.setHorizontalTextPosition(SwingConstants.CENTER);
            slot.setVerticalTextPosition(SwingConstants.BOTTOM);

            slot.addActionListener(e -> onInventorySlotClick(slotIndex));

            inventorySlots[i] = slot;
            add(slot);
        }
    }

    // 빈 슬롯을 찾아 아이템 추가 (중복 등록 방지)
    private void addItemToInventory(String itemId, String displayName, String iconBaseName) {
        for (String existing : inventoryItemIds) {
            if (itemId.equals(existing)) return;
        }
        for (int i = 0; i < INV_COLS; i++) {
            if (inventoryItemIds[i] == null) {
                inventoryItemIds[i] = itemId;
                JButton slot = inventorySlots[i];
                slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                Image img = iconBaseName != null ? loadRoomImage(iconBaseName) : null;
                if (img != null) {
                    int iconSize = INV_SLOT_SIZE - 48;
                    Image scaled = img.getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH);
                    slot.setIcon(new ImageIcon(scaled));
                    slot.setText(displayName);
                } else {
                    slot.setText("<html><center>" + displayName.replace(" ", "<br>") + "</center></html>");
                }
                slot.repaint();
                break;
            }
        }
    }

    // 인벤토리 칸 클릭 시 해당 아이템 팝업 열기
    private void onInventorySlotClick(int index) {
        String itemId = inventoryItemIds[index];
        if (itemId == null) return;

        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        switch (itemId) {
            case "ROYAL_PASS":
                new RoyalPassDialog(parentWindow).setVisible(true);
                break;
            case "ROYAL_LETTER":
                new RoyalSecretLetterDialog(parentWindow).setVisible(true);
                break;
            case "KNIGHT_CERT":
            case "WOLF_KEY":
                new KnightRewardDialog(parentWindow).setVisible(true);
                break;
            case "ARMOR_NOTE":
                new WolfArmorNoteDialog(parentWindow, this).setVisible(true);
                break;
        }
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