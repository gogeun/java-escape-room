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

public class Room2Panel extends RoomPanel {
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
            File f = common.Resources.file("resources/room2/" + baseName + ext);
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

            if (!isArmorNoteRead) markArmorNoteRead();

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
                markArmorNoteRead();
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
        if (!acquired) return;
        if (passItemButton != null) passItemButton.setVisible(!isInGame());
        reportSolved("P05");
        giveItem(new Item("I07", "왕실 인증패", "초상화 장식장에서 얻은 인증패. 뒷면에 문장 조건이 적혀 있다.",
            loadRoomImage("왕실_인증패"), () -> new RoyalPassDialog(ownerWindow()).setVisible(true)));
    }

    public boolean hasRoyalSecretLetter() {
        return hasRoyalSecretLetter;
    }

    public void setRoyalSecretLetterAcquired(boolean acquired) {
        this.hasRoyalSecretLetter = acquired;
        if (!acquired) return;
        if (letterItemButton != null) letterItemButton.setVisible(!isInGame());
        reportSolved("P06");
        giveItem(new Item("I08", "왕의 밀서", "문장 보관함에서 나온 밀서. 원탁 자리 배치 조건이 적혀 있다.",
            loadRoomImage("왕의_밀서"), () -> new RoyalSecretLetterDialog(ownerWindow()).setVisible(true)));
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
        if (!solved) return;
        if (certItemButton != null) certItemButton.setVisible(!isInGame());
        reportSolved("P07");
        Runnable view = () -> new KnightRewardDialog(ownerWindow()).setVisible(true);
        giveItem(new Item("I09", "기사 임명장", "늑대의 기사에게 내려진 임명장.",
            loadRoomImage("기사_임명장"), view));
        giveItem(new Item("I10", "늑대 문장 열쇠", "늑대의 자리 아래에 숨겨져 있던 열쇠. 갑옷 가슴의 열쇠구멍에 맞을 것 같다.",
            loadRoomImage("늑대_문장_열쇠"), view));
    }

    public boolean isSecretExitOpened() {
        return isSecretExitOpened;
    }

    public void setSecretExitOpened(boolean opened) {
        this.isSecretExitOpened = opened;
        if (opened) reportSolved("P08");
    }

    /** 늑대 갑옷 속 쪽지(O10)를 처음 확인했을 때 */
    private void markArmorNoteRead() {
        isArmorNoteRead = true;
        if (armorNoteButton != null) armorNoteButton.setVisible(!isInGame());
        giveItem(new Item("O10", "갑옷 속 쪽지", "늑대 갑옷 안에서 나온 쪽지. 출구 방향에 대한 단서가 적혀 있다.",
            loadRoomImage("갑옷_속_쪽지"), () -> new WolfArmorNoteDialog(ownerWindow(), this, false).setVisible(true)));
        updateProgress();
    }

    /** 퍼즐 오답 시 각 퍼즐 창에서 호출 -> 체력 감소 */
    public void onPuzzleFailed(String puzzleId) {
        reportWrongAnswer(puzzleId);
    }

    private Window ownerWindow() {
        return SwingUtilities.getWindowAncestor(this);
    }

    // 북쪽 문 선택 성공 시 3번 방으로 이동 처리
    public void moveToRoom3() {
        if (onMoveToRoom3Callback != null) {
            onMoveToRoom3Callback.run();
        } else if (isInGame()) {
            clearRoom();
        } else {
            JOptionPane.showMessageDialog(
                this,
                "축하합니다! 2번 방(왕실 계승의 방)의 모든 퍼즐을 풀고 [3번 방]으로 진입했습니다!",
                "2번 방 탈출 성공",
                JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    // ------------------------------------------------------------------
    // 공통 시스템(RoomPanel) 연동: 방 이름 / 목표 / 힌트 / 진행도 / 저장
    // ------------------------------------------------------------------

    @Override
    public String getRoomName() {
        return "왕실 계승의 방";
    }

    @Override
    public String getObjective() {
        if (!hasRoyalPass) {
            return "왕 초상화 장식장의 비밀을 풀어라.\n\n· 좌측 하단 독서대에 무언가 적혀 있다.\n· 초상화를 올바른 계승 순서로 누르자.";
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

    /** hints.txt 를 못 읽었을 때 쓰는 기본 힌트 (기존 코드 유지) */
    @Override
    public List<String> getCurrentHints() {
        if (!hasRoyalPass) {
            return Arrays.asList(
                "왼쪽 아래 독서대의 [왕위 계승 기록]을 읽어보세요. 초상화 장식장의 순서 단서입니다.",
                "세른 바로 다음은 베른입니다(붙어 있음). 아렌은 다렌보다 먼저지만, 둘 사이에 다른 왕이 있어야 합니다.",
                "정답 예시: 아렌 → 세른 → 베른 → 다렌  (또는 세른 → 베른 → 아렌 → 다렌)");
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