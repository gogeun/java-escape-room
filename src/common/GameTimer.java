package common;

import javax.swing.Timer;
import java.util.function.IntConsumer;

/**
 * 제한 시간 카운트다운 타이머. 1초마다 tick 콜백을 부르고 0초가 되면 timeUp 콜백을 부른다.
 * javax.swing.Timer 를 사용하므로 콜백은 항상 Swing 이벤트 스레드에서 실행된다.
 */
public class GameTimer {
    private final Timer timer;
    private int remainingSec;
    private final IntConsumer onTick;
    private final Runnable onTimeUp;

    public GameTimer(int startSec, IntConsumer onTick, Runnable onTimeUp) {
        this.remainingSec = startSec;
        this.onTick = onTick;
        this.onTimeUp = onTimeUp;
        this.timer = new Timer(1000, e -> tick());
    }

    private void tick() {
        remainingSec = Math.max(0, remainingSec - 1);
        onTick.accept(remainingSec);
        if (remainingSec == 0) {
            stop();
            onTimeUp.run();
        }
    }

    /** 시간 차감(힌트 페널티 등). 0이 되면 바로 시간 초과 처리 */
    public void subtract(int sec) {
        remainingSec = Math.max(0, remainingSec - sec);
        onTick.accept(remainingSec);
        if (remainingSec == 0) {
            stop();
            onTimeUp.run();
        }
    }

    /** 시간 추가 (개발자 모드) */
    public void add(int sec) {
        remainingSec += sec;
        onTick.accept(remainingSec);
    }

    public void start()  { timer.start(); }
    public void stop()   { timer.stop(); }
    public boolean isRunning() { return timer.isRunning(); }
    public int getRemainingSec() { return remainingSec; }

    public static String format(int sec) {
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }
}
