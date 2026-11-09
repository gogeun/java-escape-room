package room1;

import java.awt.*;
import java.awt.event.*;
import java.util.Objects;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.View;
import room1.RoomProgress.Command;
import room1.RoomProgress.Item;
import room1.RoomProgress.Station;

/** 팀 공통 창에는 이 패널을 추가하고, 생성자에 2번 방으로 이동하는 동작을 전달한다. */
public final class Room1Panel extends JPanel {
    private static final double OPENING_SECONDS = 3.0;
    private static final int HEADER_HEIGHT = 60;
    private final Runnable onComplete;
    private final RoomProgress game = new RoomProgress();
    private final WorkshopScene scene = new WorkshopScene(this::inspect);
    private final JPanel bar = new JPanel(new BorderLayout(12, 0));
    private final JPanel journal = new JPanel(new BorderLayout(10, 3));
    private final JLabel title = Brass.label("공중도시의 시간 기관실", 22, Brass.CREAM, true);
    private final JLabel status = Brass.label("", 13, Brass.GOLD, true);
    private final JLabel narration = Brass.label("붉은 경광등이 돌고 있다. 책상 위에 정비사가 펼쳐 둔 문서가 있다.", 16, Brass.CREAM, false);
    private final JButton inventory = Brass.button("소지품", this::showInventory);
    private final JButton hintButton = Brass.button("힌트", this::toggleHints);
    private final JButton soundButton = Brass.button("소리 켜짐", this::toggleSound);
    private final JPanel shade = new JPanel(new GridBagLayout()) {
        @Override protected void paintComponent(Graphics g) {
            g.setColor(new Color(11, 10, 8, 168));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    private final CardLayout sheetCards = new CardLayout();
    private final JPanel pages = new JPanel(sheetCards);
    private final JPanel sheet = new JPanel(new BorderLayout(0, 12));
    private final JPanel hintSheet = new JPanel(new BorderLayout(0, 12));
    private final JPanel body = new JPanel();
    private final JScrollPane scroll = new JScrollPane(body);
    private final JLabel sheetTitle = Brass.label("", 23, Brass.CREAM, true);
    private final JPanel buttons = Brass.row();
    private final JPanel tools = Brass.row();
    private final Timer timer = new Timer(40, e -> tick());
    private RoomMedia.Audio audio;
    private boolean muted = Boolean.getBoolean("skyborne.mute");
    private Station active;
    private boolean viewingInventory;
    private boolean completionSent;
    private boolean noticeIsError;
    private String notice = "";
    private long previousTick;
    private double openingTime, travelTime, visiblePressure = 85;
    private int sheetWidth;
    private InstrumentStrip instrument;
    private JComponent initialFocus;

    public Room1Panel() { this(() -> {}); }

    public Room1Panel(Runnable onComplete) {
        super(new BorderLayout());
        this.onComplete = Objects.requireNonNull(onComplete, "onComplete");
        setPreferredSize(new Dimension(1280, 720));
        bar.setOpaque(false);
        bar.add(title, BorderLayout.WEST);
        tools.add(soundButton);
        tools.add(inventory);
        bar.add(tools, BorderLayout.EAST);
        journal.setBackground(new Color(28, 24, 19, 239));
        journal.setBorder(Brass.frame(11));
        journal.add(status, BorderLayout.NORTH);
        journal.add(narration, BorderLayout.CENTER);
        shade.setOpaque(false);
        shade.setBorder(BorderFactory.createEmptyBorder(HEADER_HEIGHT, 0, 0, 0));
        shade.setVisible(false);
        sheet.setBackground(Brass.INK);
        sheet.setBorder(Brass.frame(20));
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Brass.INK);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(Brass.INK);
        sheet.add(sheetTitle, BorderLayout.NORTH);
        sheet.add(scroll, BorderLayout.CENTER);
        sheet.add(buttons, BorderLayout.SOUTH);
        hintSheet.setBackground(Brass.INK);
        hintSheet.setBorder(Brass.frame(20));
        pages.add(sheet, "inspect");
        pages.add(hintSheet, "hints");
        shade.add(pages);

        // 어두운 머리띠 위에 제목과 메뉴가 남도록 별도 레이어를 쓴다.
        JPanel headerShadow = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setPaint(new GradientPaint(0, 0, new Color(12, 10, 8, 205), 0, getHeight(), new Color(12, 10, 8, 0)));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.dispose();
            }
            @Override public boolean contains(int x, int y) { return false; }
        };
        headerShadow.setOpaque(false);
        JLayeredPane layers = new JLayeredPane() {
            @Override public void doLayout() {
                int w = getWidth(), h = getHeight();
                scene.setBounds(0, 0, w, h);
                headerShadow.setBounds(0, 0, w, 95);
                int margin = Math.min(25, w/32);
                Dimension hintSize = hintButton.getPreferredSize();
                int hintX = w - margin - hintSize.width;
                hintButton.setBounds(hintX, 15, hintSize.width, hintSize.height);
                bar.setBounds(margin, 12, Math.max(1, hintX-margin-6), 48);
                int footer = Math.min(840, w-2*margin);
                journal.setBounds((w-footer)/2, h-88, footer, 72);
                shade.setBounds(0, 0, w, h);
                fitSheet();
            }
        };
        layers.add(scene, Integer.valueOf(0));
        layers.add(headerShadow, Integer.valueOf(1));
        layers.add(bar, Integer.valueOf(2));
        layers.add(journal, Integer.valueOf(2));
        layers.add(shade, Integer.valueOf(3));
        layers.add(hintButton, Integer.valueOf(4));
        add(layers);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "closeSheet");
        getActionMap().put("closeSheet", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { if (shade.isVisible()) back(); }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) == 0) return;
            if (isShowing() && !completionSent) {
                previousTick = System.nanoTime();
                if (!muted) {
                    audio = new RoomMedia.Audio();
                    if (openingTime == 0 && game.solved() < 3) play("rumble");
                }
                timer.start();
            } else {
                if (timer.isRunning()) openingTime = OPENING_SECONDS;
                timer.stop();
                closeAudio();
            }
        });
        refreshBar();
    }

    private void closeAudio() {
        if (audio != null) audio.close();
        audio = null;
    }

    private void toggleSound() {
        muted = !muted;
        closeAudio();
        if (!muted && isShowing()) audio = new RoomMedia.Audio();
        refreshBar();
    }

    private void play(String name) {
        if (!muted && audio != null) audio.play(name);
    }

    private void refreshBar() {
        inventory.setText("소지품  " + game.count());
        soundButton.setText(muted ? "소리 꺼짐" : "소리 켜짐");
        String condition = game.solved() >= 3 ? "기관 정지" : game.solved() >= 2 ? "안전 압력" : "기관 폭주";
        status.setForeground(game.solved() >= 2 ? Brass.GREEN : Brass.GOLD);
        status.setText(condition + "     ·     정비 절차  " + game.solved() + " / 4");
    }

    private void tick() {
        long now = System.nanoTime();
        double elapsed = (now-previousTick)/1_000_000_000.0;
        double dt = Math.min(.15, elapsed);
        previousTick = now;
        openingTime = Math.min(OPENING_SECONDS, openingTime + elapsed);
        if (game.solved() >= 2) visiblePressure = Math.max(50, visiblePressure - 28*dt);
        if (game.solved() == 4) {
            travelTime += dt;
            scene.update(1, game.solved(), Math.min(1, travelTime/2.6), dt);
            if (travelTime >= 2.9 && !completionSent) {
                completionSent = true;
                title.setText("왕실의 비밀실");
                narration.setText("문밖엔 촛불과 낡은 왕좌가 있다. 승강기는 수백 년 전의 성에 도착했다.");
                status.setText("왕실의 비밀실 도착");
                timer.stop();
                closeAudio();
                onComplete.run();
            }
        } else {
            scene.update(openingTime / OPENING_SECONDS, game.solved(), 0, dt);
        }
        if (instrument != null) instrument.setPressure(visiblePressure);
    }

    private void inspect(Station station) {
        if (game.solved() == 4) return;
        active = station;
        viewingInventory = false;
        notice = "";
        if (station == Station.DESK && game.takeDocument()) {
            say("정비 책상에서 정비 문서를 챙겼다.", "item");
        }
        showStation();
    }

    private void say(String message, String sound) {
        notice = message;
        noticeIsError = false;
        narration.setText(message);
        play(sound);
        refreshBar();
    }

    private void showStation() {
        begin(active.name);
        switch (active) {
            case DESK:
                document(Item.I01);
                break;
            case LOCKER:
                if (game.solved() > 0) {
                    paragraph("정비함이 열려 있다. 안쪽 걸쇠에 키트가 걸려 있던 자국이 남았다.");
                } else {
                    instrument(Station.LOCKER);
                    paragraph("네 자리 황동 다이얼 아래에 ‘4차 톱니 회전 수’가 새겨져 있다.");
                    numberInput("정비함 암호", "잠금 해제", 4);
                }
                break;
            case PRESSURE:
                instrument(Station.PRESSURE);
                if (game.solved() >= 2) {
                    paragraph("안전 압력에 도달했다. 제어판 아래의 열쇠 보관함이 열렸다.");
                } else if (toolReady("압력 보정 키트 장착")) {
                    paragraph("키트 설명서   안전 압력은 50. 밸브를 한 번 돌릴 때마다 압력이 5씩 내려간다.");
                    numberInput("밸브를 돌릴 횟수", "밸브 돌리기", 1);
                }
                break;
            case STOP:
                if (game.solved() >= 3) {
                    paragraph("기관이 멈췄다. 카드 배출구와 정지 기록지 출력구가 열려 있다.");
                } else if (toolReady("해제키 돌리기")) {
                    instrument(Station.STOP);
                    procedure();
                }
                break;
            case LIFT:
                if (toolReady("비상 운행 카드 삽입")) {
                    instrument(Station.LIFT);
                    paragraph("판독기에 불이 들어왔다. 기관이 실제로 멈춘 시각을 24시간제 네 자리로 입력한다.");
                    numberInput("시간 좌표  ·  HHMM", "승강기 운행", 4);
                }
                break;
        }
        if (!notice.isEmpty()) {
            JTextArea result = prose(notice, noticeIsError ? new Color(247, 162, 137) : Brass.GREEN);
            result.setFont(Brass.font(Font.BOLD, 14));
            block(result);
        }
        buttons.add(Brass.button("소지품 열기", this::showInventory));
        buttons.add(Brass.button("조사 마치기", this::back));
        display();
    }

    private boolean toolReady(String verb) {
        Station station = active;
        if (game.installed(station.tool)) return true;
        if (!game.has(station.tool)) {
            paragraph("장치가 잠겨 있다. 필요한 물품은 ‘" + station.tool.name + "’이다.");
            return false;
        }
        paragraph("‘" + station.tool.name + "’을 장치에 연결할 수 있다.");
        JButton use = Brass.button(verb, () -> {
            if (game.install(station)) {
                say(station.tool.name + " 연결 완료. 장치가 입력을 기다린다.",
                        station == Station.PRESSURE ? "valve" : "unlock");
                showStation();
            }
        });
        block(use);
        initialFocus = use;
        return false;
    }

    private void numberInput(String label, String verb, int digits) {
        JTextField field = new JTextField(digits + 1);
        field.setFont(new Font(Font.MONOSPACED, Font.BOLD, 27));
        field.setHorizontalAlignment(JTextField.CENTER);
        field.setBackground(new Color(17, 15, 13));
        field.setForeground(Brass.GOLD);
        field.setCaretColor(Brass.CREAM);
        field.setBorder(Brass.frame(7));
        field.getAccessibleContext().setAccessibleName(label);
        InstrumentStrip face = instrument;
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { face.setDigits(field.getText()); }
            @Override public void removeUpdate(DocumentEvent e) { face.setDigits(field.getText()); }
            @Override public void changedUpdate(DocumentEvent e) { face.setDigits(field.getText()); }
        });
        JLabel caption = Brass.label(label, 15, Brass.MUTED, true);
        caption.setLabelFor(field);
        block(caption);
        JPanel line = Brass.row();
        line.add(field);
        JButton confirm = Brass.button(verb, () -> submitNumber(field, digits));
        field.addActionListener(e -> confirm.doClick());
        line.add(confirm);
        block(line);
        initialFocus = field;
    }

    private void submitNumber(JTextField field, int digits) {
        String answer = field.getText().trim();
        if (!game.submit(active, answer)) {
            notice = answer.matches("[0-9]{" + digits + "}") ? "장치가 응답하지 않는다. 단서를 다시 확인하자."
                    : "숫자 " + digits + "자리를 입력해야 한다.";
            noticeIsError = true;
            play("fail");
            showStation();
            return;
        }
        switch (active) {
            case LOCKER: say("걸쇠가 풀렸다. 압력 보정 키트를 챙겼다.", "unlock"); break;
            case PRESSURE: say("증기가 빠지며 바늘이 내려간다. 정지 패널 해제키를 얻었다.", "steam"); break;
            case LIFT:
                say("철문이 닫힌다. 층 표시기의 바늘이 시간을 거슬러 돈다.", "elevator");
                dismiss();
                tools.setVisible(false);
                return;
            default: return;
        }
        showStation();
    }

    private void instrument(Station station) {
        instrument = new InstrumentStrip(station, game, visiblePressure);
        block(instrument);
    }

    private void procedure() {
        paragraph("열쇠 꼬리표   시간축은 반드시 마지막에 잠가라.");
        paragraph("경고문 ①   연료가 흐르는 동안 냉각 장치를 켜지 말 것.\n경고문 ②   증기 배출은 냉각 바로 다음에만 가능하다.");
        paragraph("정비사 메모   빨간 비상 정지 버튼 고장. 누르면 처음부터 다시.");
        JPanel keys = new JPanel(new GridLayout(1, 5, 5, 0));
        keys.setOpaque(false);
        for (Command c : new Command[]{Command.COOL, Command.AXIS, Command.FUEL, Command.VENT, Command.EMERGENCY}) {
            keys.add(Brass.switchKey(c.label, c == Command.EMERGENCY, () -> press(c)));
        }
        block(keys);
    }

    private void press(Command command) {
        if (!game.press(command)) {
            notice = command == Command.EMERGENCY ? "고장 난 비상 정지 회로가 입력을 초기화했다."
                    : "순서가 맞지 않는다. 정지 절차를 처음부터 다시 입력한다.";
            noticeIsError = true;
            play("fail");
        } else if (game.solved() == 3) {
            say("시간축이 잠겼다. 비상 운행 카드와 기관 정지 기록을 얻었다.", "unlock");
        } else {
            say(command.label + " 완료. 다음 정지 절차를 입력한다.", "lever");
        }
        showStation();
    }

    private void showInventory() {
        viewingInventory = true;
        begin("소지품");
        if (game.count() == 0) paragraph("아직 챙긴 물품이 없다. 정비 책상을 살펴보자.");
        for (Item item : Item.values()) {
            if (!game.has(item)) continue;
            String suffix = game.installed(item) ? "   ·   장착됨" : "";
            block(Brass.button(item.name + suffix, () -> {
                begin(item.name);
                document(item);
                buttons.add(Brass.button("목록으로", this::showInventory));
                buttons.add(Brass.button("돌아가기", this::back));
                display();
            }));
        }
        buttons.add(Brass.button("돌아가기", this::back));
        display();
    }

    private void toggleHints() {
        if (shade.isVisible() && hintSheet.isVisible()) back();
        else showHintTopics();
    }

    private void showHintTopics() {
        hintSheet.removeAll();
        hintSheet.add(Brass.label("퍼즐 힌트", 23, Brass.CREAM, true), BorderLayout.NORTH);
        JPanel topics = new JPanel(new GridLayout(RoomProgress.PUZZLES.size(), 1, 0, 8));
        topics.setOpaque(false);
        for (Station puzzle : RoomProgress.PUZZLES) {
            topics.add(Brass.button(puzzle.name, () -> showHint(puzzle)));
        }
        hintSheet.add(topics, BorderLayout.CENTER);
        JPanel actions = Brass.row();
        actions.add(Brass.button("돌아가기", this::back));
        hintSheet.add(actions, BorderLayout.SOUTH);
        sheetCards.show(pages, "hints");
        display((JComponent) topics.getComponent(0));
    }

    private void showHint(Station puzzle) {
        if (game.hintLevel(puzzle) == 0) game.revealHint(puzzle);
        int level = game.hintLevel(puzzle);
        hintSheet.removeAll();
        hintSheet.add(Brass.label(puzzle.name + " 힌트   " + level + " / " + puzzle.hints.length,
                23, Brass.CREAM, true), BorderLayout.NORTH);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < level; i++) {
            if (i > 0) text.append("\n\n");
            text.append(puzzle.hints[i]);
        }
        JScrollPane notes = new JScrollPane(prose(text.toString(), Brass.BROWN));
        notes.getViewport().setBackground(Brass.PAPER);
        notes.setBorder(Brass.frame(16));
        notes.setBackground(Brass.PAPER);
        hintSheet.add(notes, BorderLayout.CENTER);
        JPanel actions = Brass.row();
        JButton backButton = Brass.button("돌아가기", this::back);
        actions.add(backButton);
        actions.add(Brass.button("퍼즐 목록", this::showHintTopics));
        if (level < puzzle.hints.length) {
            String label = level == puzzle.hints.length - 1 ? "정답 보기" : "다음 힌트";
            actions.add(Brass.button(label, () -> {
                game.revealHint(puzzle);
                showHint(puzzle);
            }));
        }
        hintSheet.add(actions, BorderLayout.SOUTH);
        display(backButton);
    }

    /** 팀 공통 점수 화면에서 사용할 수 있는, 이 방에서 새로 열어 본 힌트 단계 수. */
    public int getHintsUsed() { return game.hintsUsed(); }

    private void document(Item item) {
        JPanel paper = new JPanel();
        paper.setBackground(Brass.PAPER);
        paper.setBorder(Brass.frame(16));
        paper.setLayout(new BoxLayout(paper, BoxLayout.Y_AXIS));
        paper.add(Brass.label(item.form, 13, Brass.BROWN, true));
        paper.add(Box.createVerticalStrut(6));
        paper.add(Brass.label(item.heading, 22, Brass.BROWN, true));
        paper.add(Box.createVerticalStrut(14));
        paper.add(prose(item.text, Brass.BROWN));
        paper.add(Box.createVerticalStrut(6));
        paper.add(prose(item.note, Brass.BROWN));
        block(paper);
    }

    private void back() {
        if (hintSheet.isVisible()) {
            sheetCards.show(pages, "inspect");
            if (active != null || viewingInventory) display();
            else {
                dismiss();
                hintButton.requestFocusInWindow();
            }
            return;
        }
        if (viewingInventory && active != null) {
            viewingInventory = false;
            showStation();
        } else dismiss();
    }

    private void dismiss() {
        active = null;
        viewingInventory = false;
        instrument = null;
        shade.setVisible(false);
        boolean playing = game.solved() < 4;
        hintButton.setVisible(playing);
        tools.setVisible(playing);
        scene.targetsVisible(playing);
        if (playing) inventory.requestFocusInWindow();
    }

    private void begin(String heading) {
        body.removeAll();
        buttons.removeAll();
        instrument = null;
        initialFocus = null;
        sheetTitle.setText(heading);
    }

    private JTextArea prose(String text, Color color) {
        JTextArea p = new JTextArea(text) {
            @Override public Dimension getPreferredSize() {
                // BoxLayout의 첫 배치 전에도 실제 종이/패널 폭으로 줄 수를 계산한다.
                int width = Math.max(80, sheetWidth - 66);
                if (getParent() != null && getParent() != body) width -= 34;
                View view = getUI().getRootView(this);
                view.setSize(width, Integer.MAX_VALUE);
                int height = (int)Math.ceil(view.getPreferredSpan(View.Y_AXIS));
                return new Dimension(width, height + 2);
            }
            @Override public Dimension getMaximumSize() {
                return new Dimension(Short.MAX_VALUE, getPreferredSize().height);
            }
        };
        p.setFont(Brass.font(Font.PLAIN, 16));
        p.setOpaque(false);
        p.setForeground(color);
        p.setEditable(false);
        p.setFocusable(false);
        p.setLineWrap(true);
        p.setWrapStyleWord(true);
        p.setAlignmentX(LEFT_ALIGNMENT);
        return p;
    }

    private void paragraph(String text) { block(prose(text, Brass.CREAM)); }

    private void block(JComponent content) {
        content.setAlignmentX(LEFT_ALIGNMENT);
        if (!(content instanceof JTextArea)) {
            content.setMaximumSize(new Dimension(Short.MAX_VALUE, content.getPreferredSize().height));
        }
        body.add(content);
        body.add(Box.createVerticalStrut(10));
    }

    private void fitSheet() {
        int w = Math.max(1, Math.min(666, shade.getWidth()-32));
        int h = Math.max(1, Math.min(active == Station.STOP && !viewingInventory ? 504 : 426,
                shade.getHeight() - HEADER_HEIGHT - 32));
        if (sheetWidth != w) {
            sheetWidth = w;
            body.invalidate();
        }
        pages.setPreferredSize(new Dimension(w, h));
    }

    private void display() {
        JComponent focus = initialFocus != null ? initialFocus : (JComponent) buttons.getComponent(0);
        display(focus);
    }

    private void display(JComponent focus) {
        scene.targetsVisible(false);
        tools.setVisible(false);
        fitSheet();
        shade.setVisible(true);
        shade.revalidate();
        shade.repaint();
        scroll.getVerticalScrollBar().setValue(0);
        SwingUtilities.invokeLater(focus::requestFocusInWindow);
    }

    /** 팀 저장소에서도 별도 공통 클래스 없이 단독 실행할 수 있는 시연 진입점. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("공중도시의 시간 기관실");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(new Room1Panel());
            Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
            frame.pack();
            frame.setMinimumSize(new Dimension(Math.min(720, available.width), Math.min(480, available.height)));
            frame.setSize(Math.min(frame.getWidth(), available.width), Math.min(frame.getHeight(), available.height));
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
