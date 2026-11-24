package common;

/**
 * 게임이 끝났을 때(클리어 / 게임오버)의 결과 기록.
 */
public class GameResult {
    private final boolean cleared;
    private final String reason;
    private final String playerName;
    private final Difficulty difficulty;
    private final int score;
    private final int elapsedSec;
    private final int hp;
    private final int maxHp;
    private final int hintsUsed;
    private final int wrongCount;
    private final int puzzlesSolved;
    private final int roomsCleared;

    public GameResult(boolean cleared, String reason, String playerName, Difficulty difficulty,
                      int score, int elapsedSec, int hp, int maxHp, int hintsUsed,
                      int wrongCount, int puzzlesSolved, int roomsCleared) {
        this.cleared = cleared;
        this.reason = reason;
        this.playerName = playerName;
        this.difficulty = difficulty;
        this.score = score;
        this.elapsedSec = elapsedSec;
        this.hp = hp;
        this.maxHp = maxHp;
        this.hintsUsed = hintsUsed;
        this.wrongCount = wrongCount;
        this.puzzlesSolved = puzzlesSolved;
        this.roomsCleared = roomsCleared;
    }

    /** 점수 → 등급 */
    public String getGrade() {
        if (!cleared) return "F";
        double base = score / difficulty.getScoreMultiplier();
        if (base >= 5500) return "S";
        if (base >= 4500) return "A";
        if (base >= 3500) return "B";
        return "C";
    }

    public boolean isCleared()        { return cleared; }
    public String getReason()         { return reason; }
    public String getPlayerName()     { return playerName; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getScore()             { return score; }
    public int getElapsedSec()        { return elapsedSec; }
    public int getHp()                { return hp; }
    public int getMaxHp()             { return maxHp; }
    public int getHintsUsed()         { return hintsUsed; }
    public int getWrongCount()        { return wrongCount; }
    public int getPuzzlesSolved()     { return puzzlesSolved; }
    public int getRoomsCleared()      { return roomsCleared; }
}
