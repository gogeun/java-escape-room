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
    private boolean isSecretExitOpened = false;    // P08 북쪽 비밀 출구 개방 여부

    private JButton passItemButton;   // 왕실 인증패 다시보기 버튼
    private JButton letterItemButton; // 왕의 밀서 다시보기 버튼
    private JButton certItemButton;   // 기사 임명장 & 늑대 열쇠 다시보기 버튼
    private JButton armorNoteButton;  // 늑대 갑옷 쪽지 & 출구 안내도 다시보기 버튼

    // 3번 방으로 화면을 전환할 때 사용할 콜백 (메인 프레임에서 연결 가능)
    private Runnable onMoveToRoom3Callback;

    public Room2Panel() {
        this(null);
    }

    public Room2Panel(Runnable onMoveToRoom3Callback) {
        this.onMoveToRoom3Callback = onMoveToRoom3Callback;

        setLayout(null);
        setPreferredSize(new Dimension(1280, 720));

        backgroundImage = loadRoomImage("중세방_전체배경");

        initScrollClueButton();      // 1. 좌측 하단 독서대 단서
        initPortraitCabinetButton(); // 2. P05 좌측 벽면 왕 초상화 퍼즐
        initCrestBoxButton();        // 3. P06 중앙 좌측 벽면 문장 보관함(방패) 퍼즐
        initRoundTableButton();      // 4. P07 중앙 원탁의 빈자리 퍼즐
        initKnightArmorButton();     // 5. P08 우측 기사 갑옷(O10) -> 출구 안내도(O11) 퍼즐
        initExitMapButton();         // 6. P08 우측 벽면 출구 안내도(O11) 직접 클릭 영역
        initItemViewerButtons();     // 7. 우측 하단 획득 아이템 확인 버튼들
    }

    // .png 우선 로드 (.jpg 폴백 지원)
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

    // 1. 좌측 하단 독서대(두루마리) 클릭 영역
    private void initScrollClueButton() {
        JButton scrollButton = new JButton();
        scrollButton.setBounds(45, 295, 145, 215);
        makeTransparent(scrollButton);

        scrollButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new ScrollPopupDialog(parentWindow).setVisible(true);
        });

        add(scrollButton);
    }

    // 2. 좌측 벽면 초상화 장식장 클릭 영역 (P05 첫 번째 퍼즐)
    private void initPortraitCabinetButton() {
        JButton portraitCabinetBtn = new JButton();
        portraitCabinetBtn.setBounds(160, 150, 175, 125);
        makeTransparent(portraitCabinetBtn);

        portraitCabinetBtn.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new PortraitPuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(portraitCabinetBtn);
    }

    // 3. 벽면 붉은 방패 모양 '문장 보관함(O08)' 클릭 영역 (P06 두 번째 퍼즐)
    private void initCrestBoxButton() {
        JButton crestBoxBtn = new JButton();
        crestBoxBtn.setBounds(340, 35, 210, 245);
        makeTransparent(crestBoxBtn);

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

    // 4. 방 중앙 '원탁과 의자들(O09)' 클릭 영역 (P07 세 번째 퍼즐)
    private void initRoundTableButton() {
        JButton roundTableBtn = new JButton();
        roundTableBtn.setBounds(350, 300, 540, 230);
        makeTransparent(roundTableBtn);

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

    // 5. 방 우측 '기사 갑옷(O10)' 클릭 영역 (P08 네 번째 퍼즐: 늑대 갑옷 속 쪽지 -> 출구 안내도)
    private void initKnightArmorButton() {
        JButton armorBtn = new JButton();
        // 우측에 서 있는 기사 갑옷들 영역
        armorBtn.setBounds(895, 190, 340, 310);
        makeTransparent(armorBtn);

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

            isArmorNoteRead = true;
            if (armorNoteButton != null) {
                armorNoteButton.setVisible(true);
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new WolfArmorNoteDialog(parentWindow, this).setVisible(true);
        });

        add(armorBtn);
    }

    // 6. 우측 상단 벽면 '출구 안내도(O11)' 클릭 영역 (P08 네 번째 퍼즐 직접 진입)
    private void initExitMapButton() {
        JButton exitMapBtn = new JButton();
        exitMapBtn.setBounds(740, 120, 145, 175);
        makeTransparent(exitMapBtn);

        exitMapBtn.addActionListener(e -> {
            if (!hasKnightCertificate || !hasWolfKey) {
                JOptionPane.showMessageDialog(
                    this,
                    "동·서·남·북 네 방향의 통로가 그려진 출구 안내도다.\n" +
                    "올바른 문을 열려면 [기사 임명장]과 [늑대 문장 열쇠], 그리고 갑옷 속 단서가 필요하다.",
                    "출구 안내도 (O11)",
                    JOptionPane.INFORMATION_MESSAGE
                );
                return;
            }

            if (!isArmorNoteRead) {
                isArmorNoteRead = true;
                if (armorNoteButton != null) {
                    armorNoteButton.setVisible(true);
                }
                Window parentWindow = SwingUtilities.getWindowAncestor(this);
                new WolfArmorNoteDialog(parentWindow, this).setVisible(true);
                return;
            }

            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new SecretExitPuzzleDialog(parentWindow, this).setVisible(true);
        });

        add(exitMapBtn);
    }

    // 7. 우측 하단 획득 아이템 & 단서 다시보기 버튼 배치
    private void initItemViewerButtons() {
        passItemButton = createViewerButton("왕실 인증패 확인", 504, new Color(80, 50, 25));
        passItemButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new RoyalPassDialog(parentWindow).setVisible(true);
        });
        add(passItemButton);

        letterItemButton = createViewerButton("왕의 밀서 확인", 550, new Color(100, 35, 25));
        letterItemButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new RoyalSecretLetterDialog(parentWindow).setVisible(true);
        });
        add(letterItemButton);

        certItemButton = createViewerButton("임명장 & 열쇠 확인", 596, new Color(45, 60, 85));
        certItemButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new KnightRewardDialog(parentWindow).setVisible(true);
        });
        add(certItemButton);

        armorNoteButton = createViewerButton("출구 안내도 열기", 642, new Color(40, 75, 50));
        armorNoteButton.addActionListener(e -> {
            Window parentWindow = SwingUtilities.getWindowAncestor(this);
            new SecretExitPuzzleDialog(parentWindow, this).setVisible(true);
        });
        add(armorNoteButton);
    }

    private JButton createViewerButton(String text, int y, Color bg) {
        JButton btn = new JButton(text);
        btn.setBounds(1085, y, 170, 38);
        btn.setFont(new Font("바탕", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(new Color(255, 235, 190));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setVisible(false);
        return btn;
    }

    private void makeTransparent(JButton btn) {
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    public boolean hasRoyalPass() {
        return hasRoyalPass;
    }

    public void setRoyalPassAcquired(boolean acquired) {
        this.hasRoyalPass = acquired;
        if (acquired && passItemButton != null) {
            passItemButton.setVisible(true);
        }
    }

    public boolean hasRoyalSecretLetter() {
        return hasRoyalSecretLetter;
    }

    public void setRoyalSecretLetterAcquired(boolean acquired) {
        this.hasRoyalSecretLetter = acquired;
        if (acquired && letterItemButton != null) {
            letterItemButton.setVisible(true);
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
        if (solved && certItemButton != null) {
            certItemButton.setVisible(true);
        }
    }

    public boolean isSecretExitOpened() {
        return isSecretExitOpened;
    }

    public void setSecretExitOpened(boolean opened) {
        this.isSecretExitOpened = opened;
    }

    // 북쪽 문 선택 성공 시 3번 방으로 이동 처리
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