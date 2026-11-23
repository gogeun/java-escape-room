package common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 설정 값 (효과음, 음량, 화면 흔들림, 시간 경고음). data/settings.properties 에 유지된다.
 */
public final class GameSettings {
    private static GameSettings instance;

    public static GameSettings getInstance() {
        if (instance == null) instance = new GameSettings();
        return instance;
    }

    private static final File FILE = Resources.file("data/settings.properties");

    private boolean soundOn = true;
    private int volume = 60;            // 0 ~ 100
    private boolean screenShake = true;
    private boolean lowTimeWarning = true;
    private boolean confirmHint = true;

    private GameSettings() {
        load();
    }

    private void load() {
        if (!FILE.exists()) return;
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(FILE), StandardCharsets.UTF_8)) {
            p.load(r);
            soundOn = Boolean.parseBoolean(p.getProperty("soundOn", "true"));
            volume = Integer.parseInt(p.getProperty("volume", "60"));
            screenShake = Boolean.parseBoolean(p.getProperty("screenShake", "true"));
            lowTimeWarning = Boolean.parseBoolean(p.getProperty("lowTimeWarning", "true"));
            confirmHint = Boolean.parseBoolean(p.getProperty("confirmHint", "true"));
        } catch (Exception ignored) {
        }
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("soundOn", String.valueOf(soundOn));
        p.setProperty("volume", String.valueOf(volume));
        p.setProperty("screenShake", String.valueOf(screenShake));
        p.setProperty("lowTimeWarning", String.valueOf(lowTimeWarning));
        p.setProperty("confirmHint", String.valueOf(confirmHint));
        FILE.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8)) {
            p.store(w, "Escape Room Settings");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isSoundOn()        { return soundOn; }
    public int getVolume()            { return volume; }
    public boolean isScreenShake()    { return screenShake; }
    public boolean isLowTimeWarning() { return lowTimeWarning; }
    public boolean isConfirmHint()    { return confirmHint; }

    public void setSoundOn(boolean v)        { soundOn = v; }
    public void setVolume(int v)             { volume = Math.max(0, Math.min(100, v)); }
    public void setScreenShake(boolean v)    { screenShake = v; }
    public void setLowTimeWarning(boolean v) { lowTimeWarning = v; }
    public void setConfirmHint(boolean v)    { confirmHint = v; }
}
