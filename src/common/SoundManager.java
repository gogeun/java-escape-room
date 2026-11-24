package common;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 효과음 재생. 별도의 음원 파일 없이 사인파를 직접 만들어 재생한다.
 * (사운드 장치가 없는 환경에서도 예외 없이 조용히 넘어감)
 */
public final class SoundManager {
    private static SoundManager instance;

    public static SoundManager getInstance() {
        if (instance == null) instance = new SoundManager();
        return instance;
    }

    /** 효과음 종류: {주파수(Hz), 길이(ms)} 의 나열 */
    public enum Effect {
        CLICK    (new int[][]{{880, 25}}),
        WRONG    (new int[][]{{220, 120}, {150, 240}}),
        SUCCESS  (new int[][]{{660, 90}, {880, 90}, {1100, 170}}),
        ITEM     (new int[][]{{990, 70}, {1320, 120}}),
        TICK     (new int[][]{{1250, 40}}),
        GAME_OVER(new int[][]{{330, 220}, {262, 220}, {196, 450}}),
        CLEAR    (new int[][]{{523, 120}, {659, 120}, {784, 120}, {1047, 320}});

        final int[][] notes;
        Effect(int[][] notes) { this.notes = notes; }
    }

    private static final float SAMPLE_RATE = 22050f;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "sound");
        t.setDaemon(true);
        return t;
    });

    private SoundManager() {}

    public void play(Effect effect) {
        GameSettings s = GameSettings.getInstance();
        if (!s.isSoundOn() || s.getVolume() == 0) return;
        double vol = s.getVolume() / 100.0;
        worker.submit(() -> playNotes(effect.notes, vol));
    }

    private void playNotes(int[][] notes, double vol) {
        try {
            AudioFormat fmt = new AudioFormat(SAMPLE_RATE, 8, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(fmt)) {
                line.open(fmt);
                line.start();
                for (int[] n : notes) {
                    line.write(tone(n[0], n[1], vol), 0, (int) (SAMPLE_RATE * n[1] / 1000));
                }
                line.drain();
            }
        } catch (Exception ignored) {
            // 사운드 장치가 없으면 무시
        }
    }

    private byte[] tone(int hz, int ms, double vol) {
        int len = (int) (SAMPLE_RATE * ms / 1000);
        byte[] buf = new byte[len];
        for (int i = 0; i < len; i++) {
            double env = Math.min(1.0, Math.min(i, len - i) / (SAMPLE_RATE * 0.005)); // 클릭 노이즈 방지
            buf[i] = (byte) (Math.sin(2 * Math.PI * hz * i / SAMPLE_RATE) * 90 * vol * env);
        }
        return buf;
    }
}
