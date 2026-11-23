package common;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import mgr.Manager;

/**
 * 게임 전체 흐름을 관리하는 싱글톤 클래스.
 *
 *  - 체력 / 제한 시간 / 힌트 / 점수 / 인벤토리 상태 보관
 *  - 방 전환 (1번 방 → 2번 방 → 3번 방 → 탈출 성공)
 *  - 게임 오버 / 클리어 판정
 *  - 상태가 바뀌면 등록된 GameListener 들에게 알림 (옵저버 패턴)
 */
public class GameManager {

    private static GameManager instance;

    public static GameManager getInstance() {
        if (instance == null) instance = new GameManager();
        return instance;
    }

    private GameManager() {}

    public static final int ROOM_CLEAR_BONUS = 500;
    public static final int PUZZLE_SCORE = 300;

    private MainFrame frame;
    private List<Supplier<RoomPanel>> roomFactories = new ArrayList<>();
    private final List<GameListener> listeners = new ArrayList<>();
    private final Manager<Item> inventory = new Manager<>();   // 강의 Manager<T> 구조 사용
    private final Map<String, Integer> hintLevels = new HashMap<>();

    private PlayerStatus status;
    private GameTimer timer;
    private RoomPanel currentRoom;
    private int roomIndex;
    private int roomsCleared;
    private boolean running;
    private boolean paused;

    // ------------------------------------------------------------------
    // 초기 설정
    // ------------------------------------------------------------------

    void attachFrame(MainFrame frame) {
        this.frame = frame;
    }

    /** 방 생성 방법을 순서대로 등록 (1번 방, 2번 방, 3번 방 ...) */
    public void setRoomFactories(List<Supplier<RoomPanel>> factories) {
        this.roomFactories = new ArrayList<>(factories);
    }

    public void addListener(GameListener l)    { if (!listeners.contains(l)) listeners.add(l); }
    public void removeListener(GameListener l) { listeners.remove(l); }

    // ------------------------------------------------------------------
    // 게임 시작 / 이어하기
    // ------------------------------------------------------------------

    public void startNewGame(String playerName, Difficulty difficulty) {
        status = new PlayerStatus(playerName, difficulty);
        hintLevels.clear();
        roomsCleared = 0;
        createTimer(difficulty.getTimeLimitSec());
        running = true;
        paused = false;
        frame.showGameScreen();
        enterRoom(0);
        timer.start();
    }

    public boolean continueGame() {
        Properties p = SaveManager.load();
        if (p == null) return false;
        try {
            Difficulty d = Difficulty.valueOf(p.getProperty("difficulty", "NORMAL"));
            status = new PlayerStatus(p.getProperty("name", "플레이어"), d);
            status.restore(
                Integer.parseInt(p.getProperty("hp")),
                Integer.parseInt(p.getProperty("hintsLeft")),
                Integer.parseInt(p.getProperty("hintsUsed")),
                Integer.parseInt(p.getProperty("wrongCount")));
            status.setMemo(p.getProperty("memo", ""));
            for (String id : p.getProperty("solved", "").split(",")) {
                if (!id.isEmpty()) status.markSolved(id);
            }
            roomsCleared = Integer.parseInt(p.getProperty("roomsCleared", "0"));
            hintLevels.clear();
            createTimer(Integer.parseInt(p.getProperty("timeLeft")));
            running = true;
            paused = false;
            frame.showGameScreen();
            enterRoom(Integer.parseInt(p.getProperty("roomIndex", "0")));
            currentRoom.loadState(p);
            fireProgress();
            timer.start();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            running = false;
            return false;
        }
    }

    private void createTimer(int seconds) {
        if (timer != null) timer.stop();
        timer = new GameTimer(seconds, sec -> {
            for (GameListener l : copyListeners()) l.onTimeChanged(sec);
            if (sec > 0 && sec <= 60 && sec % 10 == 0 && GameSettings.getInstance().isLowTimeWarning()) {
                SoundManager.getInstance().play(SoundManager.Effect.TICK);
            }
        }, () -> gameOver("제한 시간이 모두 지났습니다."));
    }

    private void enterRoom(int index) {
        roomIndex = index;
        inventory.clear();          // 아이템은 방마다 따로 관리
        currentRoom = roomFactories.get(index).get();
        frame.setRoom(currentRoom);
        for (GameListener l : copyListeners()) {
            l.onRoomChanged(currentRoom, roomIndex, roomFactories.size());
            l.onInventoryChanged();
            l.onHpChanged(status.getHp(), status.getMaxHp());
            l.onHintsChanged(status.getHintsLeft());
            l.onTimeChanged(timer.getRemainingSec());
        }
        currentRoom.onEnter();
    }

    // ------------------------------------------------------------------
    // 방에서 호출하는 이벤트
    // ------------------------------------------------------------------

    /** 오답 처리: 체력 1 감소, 0이 되면 게임 오버 */
    public void wrongAnswer(String puzzleId) {
        if (!running) return;
        status.damage();
        SoundManager.getInstance().play(SoundManager.Effect.WRONG);
        for (GameListener l : copyListeners()) {
            l.onHpChanged(status.getHp(), status.getMaxHp());
            l.onDamaged();
        }
        if (status.isDead()) {
            gameOver("체력이 모두 소진되었습니다. (오답 " + status.getWrongCount() + "회)");
        }
    }

    public void puzzleSolved(String puzzleId) {
        if (!running) return;
        if (status.markSolved(puzzleId)) {
            SoundManager.getInstance().play(SoundManager.Effect.SUCCESS);
        }
        fireProgress();
    }

    public void addItem(Item item) {
        if (!running || inventory.find(item.getId()) != null) return;
        inventory.addElement(item);
        SoundManager.getInstance().play(SoundManager.Effect.ITEM);
        for (GameListener l : copyListeners()) l.onInventoryChanged();
    }

    public void removeItem(String itemId) {
        if (inventory.removeElement(itemId)) {
            for (GameListener l : copyListeners()) l.onInventoryChanged();
        }
    }

    public boolean hasItem(String itemId) {
        return inventory.find(itemId) != null;
    }

    /** 현재 방 탈출 → 다음 방 (마지막 방이면 게임 클리어) */
    public void nextRoom() {
        if (!running) return;
        roomsCleared++;
        if (roomIndex + 1 >= roomFactories.size()) {
            gameClear();
        } else {
            SoundManager.getInstance().play(SoundManager.Effect.CLEAR);
            closeAllDialogs();
            enterRoom(roomIndex + 1);
            SaveManager.save(this);   // 방 이동 시 자동 저장
        }
    }

    // ------------------------------------------------------------------
    // 힌트
    // ------------------------------------------------------------------

    private String hintKey(List<String> hints) {
        return roomIndex + ":" + (hints.isEmpty() ? "" : hints.get(0));
    }

    /** 현재 단계에서 이미 공개한 힌트 목록 (다시 보기는 무료) */
    public List<String> getRevealedHints() {
        List<String> hints = currentRoom.getHints();
        int level = hintLevels.getOrDefault(hintKey(hints), 0);
        return hints.subList(0, Math.min(level, hints.size()));
    }

    public boolean hasMoreHints() {
        List<String> hints = currentRoom.getHints();
        return hintLevels.getOrDefault(hintKey(hints), 0) < hints.size();
    }

    /** 다음 힌트 공개: 힌트 1개 소모 + 시간 페널티. 성공 시 true */
    public boolean revealNextHint() {
        if (!hasMoreHints() || !status.useHint()) return false;
        String key = hintKey(currentRoom.getHints());
        hintLevels.merge(key, 1, Integer::sum);
        for (GameListener l : copyListeners()) l.onHintsChanged(status.getHintsLeft());
        timer.subtract(status.getDifficulty().getHintPenaltySec());
        return true;
    }

    // ------------------------------------------------------------------
    // 일시정지 / 종료
    // ------------------------------------------------------------------

    public void pause() {
        if (running && !paused) {
            paused = true;
            timer.stop();
        }
    }

    public void resume() {
        if (running && paused) {
            paused = false;
            timer.start();
        }
    }

    public void gameOver(String reason) {
        if (!running) return;
        running = false;
        timer.stop();
        SaveManager.delete();
        SoundManager.getInstance().play(SoundManager.Effect.GAME_OVER);
        GameResult result = buildResult(false, reason);
        SwingUtilities.invokeLater(() -> {
            closeAllDialogs();
            frame.showResult(result);
        });
    }

    private void gameClear() {
        running = false;
        timer.stop();
        SaveManager.delete();
        SoundManager.getInstance().play(SoundManager.Effect.CLEAR);
        GameResult result = buildResult(true, "모든 방을 탈출했습니다!");
        RankingManager.addEntry(result);
        SwingUtilities.invokeLater(() -> {
            closeAllDialogs();
            frame.showResult(result);
        });
    }

    /** 게임을 그만두고 타이틀로 (저장 여부 선택) */
    public void quitToTitle(boolean save) {
        if (running && save) SaveManager.save(this);
        running = false;
        paused = false;
        if (timer != null) timer.stop();
        closeAllDialogs();
        frame.showTitle();
    }

    public void restart() {
        Difficulty d = status.getDifficulty();
        String name = status.getName();
        quitToTitle(false);
        startNewGame(name, d);
    }

    private GameResult buildResult(boolean cleared, String reason) {
        Difficulty d = status.getDifficulty();
        int remaining = timer.getRemainingSec();
        int elapsed = d.getTimeLimitSec() - remaining;
        int score = 0;
        if (cleared) {
            score += remaining * 2;
            score += status.getHp() * 200;
        }
        score += status.getSolvedPuzzles().size() * PUZZLE_SCORE;
        score += roomsCleared * ROOM_CLEAR_BONUS;
        score -= status.getHintsUsed() * 150;
        score -= status.getWrongCount() * 50;
        score = (int) Math.max(0, Math.round(score * d.getScoreMultiplier()));
        return new GameResult(cleared, reason, status.getName(), d, score, elapsed,
            status.getHp(), status.getMaxHp(), status.getHintsUsed(),
            status.getWrongCount(), status.getSolvedPuzzles().size(), roomsCleared);
    }

    public static void closeAllDialogs() {
        for (Window w : Window.getWindows()) {
            if (w instanceof Dialog && w.isShowing()) w.dispose();
        }
    }

    /** 방의 진행 상태가 바뀌었을 때 목표 문구 / 진행도를 다시 그리도록 알림 */
    public void notifyProgress() {
        fireProgress();
    }

    private void fireProgress() {
        for (GameListener l : copyListeners()) l.onProgressChanged();
    }

    private List<GameListener> copyListeners() {
        return new ArrayList<>(listeners);
    }

    // ------------------------------------------------------------------
    // getter
    // ------------------------------------------------------------------

    public boolean isRunning()        { return running; }
    public boolean isPaused()         { return paused; }
    public PlayerStatus getStatus()   { return status; }
    public RoomPanel getCurrentRoom() { return currentRoom; }
    public int getRoomIndex()         { return roomIndex; }
    public int getRoomCount()         { return roomFactories.size(); }
    public int getRoomsCleared()      { return roomsCleared; }
    public int getRemainingSec()      { return timer == null ? 0 : timer.getRemainingSec(); }
    public List<Item> getInventory()  { return Collections.unmodifiableList(inventory.mList); }
    /** 인벤토리에서 이름/아이디로 검색 (Manager.findAll) */
    public List<Item> searchInventory(String kwd) { return inventory.findAll(kwd); }
    public MainFrame getFrame()       { return frame; }
}
