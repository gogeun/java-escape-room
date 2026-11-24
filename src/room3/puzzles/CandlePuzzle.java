package room3.puzzles;

public final class CandlePuzzle {
    private static final int[] ANSWER={2,3,5,1,4};
    private int progress;
    public boolean press(int candle) {
        if(solved()) return true;
        if(candle==ANSWER[progress]) { progress++;return true; }
        progress=0;return false;
    }
    public boolean lit(int candle) { for(int i=0;i<progress;i++) if(ANSWER[i]==candle) return true;return false; }
    public boolean solved() { return progress==ANSWER.length; }
}
