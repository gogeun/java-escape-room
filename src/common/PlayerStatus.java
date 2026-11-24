package common;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 플레이어 한 명의 진행 상태 (체력, 힌트, 오답 횟수, 해결한 퍼즐 등).
 * 화면(UI)과 분리된 순수 데이터 클래스.
 */
public class PlayerStatus {
    private String name;
    private Difficulty difficulty;
    private int hp;
    private int hintsLeft;
    private int hintsUsed;
    private int wrongCount;
    private String memo = "";
    private final Set<String> solvedPuzzles = new LinkedHashSet<>();

    public PlayerStatus(String name, Difficulty difficulty) {
        this.name = name;
        this.difficulty = difficulty;
        this.hp = difficulty.getMaxHp();
        this.hintsLeft = difficulty.getHintCount();
    }

    /** 체력 1 감소. 남은 체력을 반환 */
    public int damage() {
        if (hp > 0) hp--;
        wrongCount++;
        return hp;
    }

    public boolean useHint() {
        if (hintsLeft <= 0) return false;
        hintsLeft--;
        hintsUsed++;
        return true;
    }

    /** 처음 해결한 퍼즐이면 true */
    public boolean markSolved(String puzzleId) {
        return solvedPuzzles.add(puzzleId);
    }

    public boolean isDead() { return hp <= 0; }

    /** 체력 가득 회복 (개발자 모드) */
    public void restoreHp() { hp = difficulty.getMaxHp(); }

    public String getName()           { return name; }
    public Difficulty getDifficulty() { return difficulty; }
    public int getHp()                { return hp; }
    public int getMaxHp()             { return difficulty.getMaxHp(); }
    public int getHintsLeft()         { return hintsLeft; }
    public int getHintsUsed()         { return hintsUsed; }
    public int getWrongCount()        { return wrongCount; }
    public String getMemo()           { return memo; }
    public Set<String> getSolvedPuzzles() { return solvedPuzzles; }

    public void setMemo(String memo)  { this.memo = memo == null ? "" : memo; }

    // ---- 저장/불러오기용 setter ----
    void restore(int hp, int hintsLeft, int hintsUsed, int wrongCount) {
        this.hp = hp;
        this.hintsLeft = hintsLeft;
        this.hintsUsed = hintsUsed;
        this.wrongCount = wrongCount;
    }
}
