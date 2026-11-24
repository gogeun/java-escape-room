package common;

/**
 * 난이도. 난이도마다 제한 시간 / 최대 체력 / 힌트 개수 / 힌트 페널티 / 점수 배율이 다르다.
 */
public enum Difficulty {
    EASY  ("쉬움",   40 * 60, 7, 5, 30,  0.8),
    NORMAL("보통",   30 * 60, 5, 3, 60,  1.0),
    HARD  ("어려움", 20 * 60, 3, 1, 120, 1.5);

    private final String label;
    private final int timeLimitSec;
    private final int maxHp;
    private final int hintCount;
    private final int hintPenaltySec;
    private final double scoreMultiplier;

    Difficulty(String label, int timeLimitSec, int maxHp, int hintCount,
               int hintPenaltySec, double scoreMultiplier) {
        this.label = label;
        this.timeLimitSec = timeLimitSec;
        this.maxHp = maxHp;
        this.hintCount = hintCount;
        this.hintPenaltySec = hintPenaltySec;
        this.scoreMultiplier = scoreMultiplier;
    }

    public String getLabel()          { return label; }
    public int getTimeLimitSec()      { return timeLimitSec; }
    public int getMaxHp()             { return maxHp; }
    public int getHintCount()         { return hintCount; }
    public int getHintPenaltySec()    { return hintPenaltySec; }
    public double getScoreMultiplier(){ return scoreMultiplier; }

    public String describe() {
        return String.format("%s  |  제한시간 %d분 · 체력 %d · 힌트 %d개",
            label, timeLimitSec / 60, maxHp, hintCount);
    }

    @Override
    public String toString() { return label; }
}
