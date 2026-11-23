package room2;

import common.Item;
import common.RoomPanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * 2번 방 - 왕실 계승의 방.
 * [통합] common.RoomPanel 을 상속해 공통 시스템(체력 / 힌트 / 인벤토리 / 진행도 / 저장)과 연결된다.
 *        게임 안에서는 공통 상단 바 / 가방을 쓰므로 이 방의 자체 가방과 소리 버튼은 숨기고,
 *        방만 따로 실행(main)할 때는 그대로 보인다.
 */
public class Room2Panel extends RoomPanel {
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

        backgroundImage = loadRoomImage("왕실_계승의_방_배경");   // 배경 그림 (1672x941, 방 화면 1280x720 과 같은 16:9)

        initScrollClueButton();      // 1. 좌측 하단 독서대 단서 열람
        initPortraitCabinetButton(); // 2. 좌측 벽면 초상화 장식장 (단순 클릭 -> 왕실 인증패 즉시 획득)
        initCrestBoxButton();        // 3. [퍼즐 1] P06 중앙 좌측 벽면 문장 보관함(방패) 퍼즐
        initRoundTableButton();      // 4. [퍼즐 2] P07 중앙 원탁의 빈자리 퍼즐
        initKnightArmorButton();     // 5. 우측 기사 갑옷 (단순 클릭 -> 갑옷 속 쪽지 즉시 획득)
        initExitMapButton();         // 6. 우측 벽면 출구 안내도 (단순 클릭 -> 비밀 출구 개방 및 3번 방 이동)
        initSoundButton();           // 7. 우측 상단 음소거 버튼
        initInventoryBar();          // 8. 하단 1줄 8칸 정사각형 인벤토리 바

        if (isInGame()) {            // [통합] 게임 중에는 공통 상단 바(설정) / 가방 사용
            soundButton.setVisible(false);
            for (JButton slot : inventorySlots) slot.setVisible(false);
        }
    }

    // .png 및 기타 확장자 지원
    public static Image loadRoomImage(String baseName) {
        String[] exts = {".png", ".jpg", ".jpeg"};
        for (String ext : exts) {
            File f = common.Resources.file("resources/room2/" + baseName + ext);   // [통합] 실행 폴더와 상관없이 찾기
            if (f.exists()) {
                try {
                    return ImageIO.read(f);
                } catch (Exception ignored) {
                }
            }
        }
        System.err.println("[이미지 없음] " + common.Resources.file("resources/room2/" + baseName + ".png").getAbsolutePath());
        return null;
    }

private void initScrollClueButton() {
        JButton scrollButton = new JButton();
        scrollButton.setBounds(55, 305, 145, 75);
        makeMagnifierButton(scrollButton, "왕위 계승 기록", 0.5, 0.5);

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
        makeMagnifierButton(portraitCabinetBtn, "왕의 초상화 장식장", 0.5, 0.5);
        portraitCabinetBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            if (!hasRoyalPass) {
                setRoyalPassAcquired(true);
                common.DialogBox.message(this, "단서 획득", "역대 왕들의 초상화 장식장 아래 비밀 홈에서\n[왕실 인증패]를 발견했습니다!");
            }
            new RoyalPassDialog(parentWindow).setVisible(true);
        });

        add(portraitCabinetBtn);
    }

    // 3. 벽면 붉은 방패 모양 '문장 보관함(O08)' 클릭 영역 ([퍼즐 1] P06)
    private void initCrestBoxButton() {
        JButton crestBoxBtn = new JButton();
        crestBoxBtn.setBounds(340, 35, 210, 245);
        makeMagnifierButton(crestBoxBtn, "왕가의 문장", 0.5, 0.45);

        crestBoxBtn.addActionListener(e -> {
            if (!hasRoyalPass) {
                common.DialogBox.message(this, "문장 보관함", "세 개의 홈이 파인 왕가의 문장 보관함이다.\n보관함을 조사하려면 먼저 [왕실 인증패]가 필요할 것 같다.");
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
        makeMagnifierButton(roundTableBtn, "원탁의 자리", 0.5, 0.4);

        roundTableBtn.addActionListener(e -> {
            if (!hasRoyalSecretLetter) {
                common.DialogBox.message(this, "원탁", "왕좌 앞에 놓인 원탁과 다섯 개의 빈 의자다.\n자리 배치의 비밀이 적힌 [왕의 밀서]를 먼저 찾아야 할 것 같다.");
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
        makeMagnifierButton(armorBtn, "기사 갑옷", 0.5, 0.45);

        armorBtn.addActionListener(e -> {
            if (!hasKnightCertificate || !hasWolfKey) {
                common.DialogBox.message(this, "기사 갑옷 (O10)", "복도에 줄지어 서 있는 기사 갑옷들이다.\n" +
                    "가슴에 늑대 문장 열쇠구멍이 있는 갑옷을 열려면\n" +
                    "먼저 원탁의 퍼즐을 풀어 [기사 임명장]과 [늑대 문장 열쇠]를 찾아야 한다.");
                return;
            }

            if (!isArmorNoteRead) {
                markArmorNoteRead();   // [통합] 쪽지 획득 + 공통 가방 등록 + 목표 갱신
                common.DialogBox.message(this, "단서 획득", "[늑대 문장 열쇠]로 갑옷의 가슴판을 열었습니다.\n안쪽에서 [갑옷 속 쪽지]를 발견했습니다!");
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new WolfArmorNoteDialog(parentWindow, this).setVisible(true);
        });

        add(armorBtn);
    }

    private void initExitMapButton() {
        JButton exitMapBtn = new JButton();
        exitMapBtn.setBounds(1070, 80, 175, 230);
        makeMagnifierButton(exitMapBtn, "출구 안내도", 0.5, 0.78);   // 오른쪽 위 목표 상자에 가리지 않게 아래쪽에 표시

        exitMapBtn.addActionListener(e -> {
            if (!hasKnightCertificate || !hasWolfKey) {
                common.DialogBox.message(this, "출구 안내도 (O11)", "동·서·남·북 네 방향의 통로가 그려진 출구 안내도다.\n" +
                    "비밀 통로를 열려면 먼저 [기사 임명장]과 [늑대 문장 열쇠]가 필요하다.");
                return;
            }

            if (!isArmorNoteRead) {
                common.DialogBox.message(this, "출구 안내도 (O11)", "출구 안내도 옆에 늑대 문장이 새겨진 잠금장치가 있다.\n" +
                    "먼저 우측의 [기사 갑옷]을 조사해 단서를 확인해야 할 것 같다.");
                return;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new SecretExitPuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(exitMapBtn);
    }


    /**
     * 조사 지점 버튼에 돋보기 표시를 붙인다.
     * [통합] 1번 방과 같은 돋보기 그림(common.Magnifier, resources/room1/magnifier*.png)을 쓰고,
     *        마우스를 올리면 물건 이름표가 뜬다. (fx, fy = 버튼 안에서 돋보기 위치 비율)
     */
    private void makeMagnifierButton(JButton btn, String label, double fx, double fy) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        common.Magnifier.decorate(btn, label, fx, fy);
    }

    public boolean hasRoyalPass() {
        return hasRoyalPass;
    }

    public void setRoyalPassAcquired(boolean acquired) {
        this.hasRoyalPass = acquired;
        if (acquired) {
            addItemToInventory("ROYAL_PASS", "왕실 인증패", "왕실_인증패");
            reportSolved("P05");   // [통합] 진행도 / 점수
            giveItem(new Item("I07", "왕실 인증패", "초상화 장식장에서 얻은 인증패. 뒷면에 문장 조건이 적혀 있다.",
                loadRoomImage("왕실_인증패"), () -> new RoyalPassDialog(ownerWindow()).setVisible(true)));
        }
    }

    public boolean hasRoyalSecretLetter() {
        return hasRoyalSecretLetter;
    }

    public void setRoyalSecretLetterAcquired(boolean acquired) {
        this.hasRoyalSecretLetter = acquired;
        if (acquired) {
            addItemToInventory("ROYAL_LETTER", "왕의 밀서", "왕의_밀서");
            reportSolved("P06");
            giveItem(new Item("I08", "왕의 밀서", "문장 보관함에서 나온 밀서. 원탁 자리 배치 조건이 적혀 있다.",
                loadRoomImage("왕의_밀서"), () -> new RoyalSecretLetterDialog(ownerWindow()).setVisible(true)));
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
            reportSolved("P07");
            Runnable view = () -> new KnightRewardDialog(ownerWindow()).setVisible(true);
            giveItem(new Item("I09", "기사 임명장", "늑대의 기사에게 내려진 임명장.",
                loadRoomImage("기사_임명장"), view));
            giveItem(new Item("I10", "늑대 문장 열쇠", "늑대의 자리 아래에 숨겨져 있던 열쇠. 갑옷 가슴의 열쇠구멍에 맞을 것 같다.",
                loadRoomImage("늑대_문장_열쇠"), view));
        }
    }

    public boolean isSecretExitOpened() {
        return isSecretExitOpened;
    }

    public void setSecretExitOpened(boolean opened) {
        this.isSecretExitOpened = opened;
        if (opened) reportSolved("P08");   // [통합] 진행도 / 점수
    }

    /** [통합] 늑대 갑옷 속 쪽지(O10)를 처음 확인했을 때: 자체 가방 + 공통 가방에 등록 */
    private void markArmorNoteRead() {
        isArmorNoteRead = true;
        addItemToInventory("ARMOR_NOTE", "갑옷 속 쪽지", "갑옷_속_쪽지");
        giveItem(new Item("O10", "갑옷 속 쪽지", "늑대 갑옷 안에서 나온 쪽지. 출구 방향에 대한 단서가 적혀 있다.",
            loadRoomImage("갑옷_속_쪽지"), () -> new WolfArmorNoteDialog(ownerWindow(), this).setVisible(true)));
        updateProgress();
    }

    /** [통합] 퍼즐 오답 시 각 퍼즐 창에서 호출 -> 체력 감소 */
    public void onPuzzleFailed(String puzzleId) {
        reportWrongAnswer(puzzleId);
    }

    private Window ownerWindow() {
        return SwingUtilities.getWindowAncestor(this);
    }

    // 3번 방으로 이동 처리
    public void moveToRoom3() {
        if (onMoveToRoom3Callback != null) {
            onMoveToRoom3Callback.run();
        } else if (isInGame()) {
            clearRoom();   // [통합] 게임 중이면 다음 방으로 이동
        } else {
            common.DialogBox.message(this, "2번 방 탈출 성공", "축하합니다! 2번 방(왕실 계승의 방)의 모든 퍼즐을 풀고 [3번 방]으로 진입했습니다!");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundImage != null) {
            common.FastImage.draw((Graphics2D) g, backgroundImage, 0, 0, getWidth(), getHeight());   // [통합] 배경 확대본 재사용
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

    // ------------------------------------------------------------------
    // [통합] 공통 시스템(RoomPanel) 연동: 방 이름 / 테마 / 목표 / 힌트 / 진행도 / 저장 / 개발자 모드
    // ------------------------------------------------------------------

    @Override
    public String getRoomName() {
        return "왕실 계승의 방";
    }

    @Override
    public common.RoomTheme getTheme() {
        return common.RoomTheme.ROYAL;   // 금박 + 진홍 벨벳 UI
    }

    @Override
    public String getObjective() {
        if (!hasRoyalPass) {
            return "왕실의 단서를 찾아라.\n\n· 왼쪽 벽, 역대 왕들의 초상화 장식장을 살펴보자.";
        } else if (!hasRoyalSecretLetter) {
            return "붉은 방패 모양의 문장 보관함을 열어라.\n\n· [왕실 인증패] 뒷면의 조건을 확인하자.\n· 세 개의 홈에 맞는 문장을 끼우자.";
        } else if (!hasWolfKey) {
            return "원탁의 빈자리에 기사들을 앉혀라.\n\n· [왕의 밀서]의 다섯 조건을 확인하자.\n· 문장 방패를 의자로 끌어다 놓자.";
        } else if (!isArmorNoteRead) {
            return "늑대 문장 열쇠가 맞는 곳을 찾아라.\n\n· 오른쪽 기사 갑옷들을 살펴보자.";
        } else if (!isSecretExitOpened) {
            return "출구 안내도에서 올바른 문을 골라라.\n\n· 갑옷 속 쪽지의 단서를 떠올리자.\n· 틀린 문을 고르면 체력이 줄어든다.";
        }
        return "비밀 출구가 열렸다! 다음 방으로 이동하자.";
    }

    @Override
    protected String getHintRoomKey() {
        return "room2";   // resources/data/hints.txt 의 room2 힌트 사용
    }

    /** hints.txt 의 단계 번호 (아래 getCurrentHints() 의 if 순서와 같음) */
    @Override
    protected int getHintStage() {
        if (!hasRoyalPass) return 1;
        if (!hasRoyalSecretLetter) return 2;
        if (!hasWolfKey) return 3;
        if (!isArmorNoteRead) return 4;
        if (!isSecretExitOpened) return 5;
        return 6;
    }

    /** hints.txt 를 못 읽었을 때 쓰는 기본 힌트 */
    @Override
    public List<String> getCurrentHints() {
        if (!hasRoyalPass) {
            return Arrays.asList(
                "왼쪽 벽에 걸린 역대 왕들의 초상화 장식장을 조사해 보세요.",
                "초상화 장식장 아래 비밀 홈에 [왕실 인증패]가 숨겨져 있습니다.");
        } else if (!hasRoyalSecretLetter) {
            return Arrays.asList(
                "[왕실 인증패] 뒷면 조건을 보세요. 홈은 3개뿐이고, 짐승(사자·독수리)은 하나만 넣을 수 있습니다.",
                "독수리를 넣으면 검 → 방패 → 장미 → 왕관까지 줄줄이 필요해서 홈이 모자랍니다. 짐승은 사자입니다.",
                "정답: 사자 + 장미 + 왕관");
        } else if (!hasWolfKey) {
            return Arrays.asList(
                "왕좌는 12시 방향입니다. ⑤번 조건 — 왕좌와 마주 보는 6시 자리가 '나의 기사'입니다.",
                "늑대는 6시, 사슴은 늑대의 시계 방향 다음인 8시입니다. 곰은 왕좌 옆(2시·10시)에 앉지 않습니다.",
                "정답: 2시 독수리 · 4시 곰 · 6시 늑대 · 8시 사슴 · 10시 사자");
        } else if (!isArmorNoteRead) {
            return Arrays.asList(
                "[늑대 문장 열쇠]는 오른쪽에 서 있는 기사 갑옷 중 하나에 맞습니다.",
                "오른쪽 기사 갑옷들을 클릭해 보세요.");
        } else if (!isSecretExitOpened) {
            return Arrays.asList(
                "쪽지에 따르면 동쪽은 무기고, 그 맞은편(서쪽)은 막다른 길, 남쪽은 무너졌습니다.",
                "남은 방향은 하나뿐입니다 — 북쪽 문을 고르세요.");
        }
        return Arrays.asList("출구가 열렸습니다. 다음 방으로 이동하세요.");
    }

    /** 개발자 모드: 다음 퍼즐 하나를 바로 해결 (마지막이면 3번 방으로 이동) */
    @Override
    public boolean devSkipPuzzle() {
        if (!hasRoyalPass) setRoyalPassAcquired(true);
        else if (!hasRoyalSecretLetter) setRoyalSecretLetterAcquired(true);
        else if (!hasWolfKey) setKnightPuzzleSolved(true);
        else if (!isArmorNoteRead) markArmorNoteRead();
        else if (!isSecretExitOpened) { setSecretExitOpened(true); moveToRoom3(); }
        else return false;
        updateProgress();
        repaint();
        return true;
    }

    @Override
    public int getTotalPuzzles() {
        return 4;
    }

    @Override
    public int getSolvedCount() {
        int n = 0;
        if (hasRoyalPass) n++;
        if (hasRoyalSecretLetter) n++;
        if (hasWolfKey) n++;
        if (isSecretExitOpened) n++;
        return n;
    }

    @Override
    public void saveState(Properties p) {
        p.setProperty("room2.royalPass", String.valueOf(hasRoyalPass));
        p.setProperty("room2.secretLetter", String.valueOf(hasRoyalSecretLetter));
        p.setProperty("room2.knight", String.valueOf(hasWolfKey));
        p.setProperty("room2.armorNote", String.valueOf(isArmorNoteRead));
    }

    @Override
    public void loadState(Properties p) {
        if (Boolean.parseBoolean(p.getProperty("room2.royalPass"))) setRoyalPassAcquired(true);
        if (Boolean.parseBoolean(p.getProperty("room2.secretLetter"))) setRoyalSecretLetterAcquired(true);
        if (Boolean.parseBoolean(p.getProperty("room2.knight"))) setKnightPuzzleSolved(true);
        if (Boolean.parseBoolean(p.getProperty("room2.armorNote"))) markArmorNoteRead();
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
