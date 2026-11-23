package common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 저장 / 불러오기 (data/save.properties).
 * 공통 상태(체력, 남은 시간, 힌트, 메모 등)는 여기서 저장하고,
 * 방마다 다른 진행 상태는 RoomPanel.saveState() 에서 직접 추가한다.
 */
public final class SaveManager {
    private SaveManager() {}

    private static final File SAVE_FILE = Resources.file("data/save.properties");

    public static boolean save(GameManager gm) {
        if (!gm.isRunning()) return false;
        PlayerStatus s = gm.getStatus();
        Properties p = new Properties();
        p.setProperty("name", s.getName());
        p.setProperty("difficulty", s.getDifficulty().name());
        p.setProperty("hp", String.valueOf(s.getHp()));
        p.setProperty("hintsLeft", String.valueOf(s.getHintsLeft()));
        p.setProperty("hintsUsed", String.valueOf(s.getHintsUsed()));
        p.setProperty("wrongCount", String.valueOf(s.getWrongCount()));
        p.setProperty("memo", s.getMemo());
        p.setProperty("solved", String.join(",", s.getSolvedPuzzles()));
        p.setProperty("timeLeft", String.valueOf(gm.getRemainingSec()));
        p.setProperty("roomIndex", String.valueOf(gm.getRoomIndex()));
        p.setProperty("roomsCleared", String.valueOf(gm.getRoomsCleared()));
        gm.getCurrentRoom().saveState(p);

        SAVE_FILE.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(SAVE_FILE), StandardCharsets.UTF_8)) {
            p.store(w, "Escape Room Save");
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Properties load() {
        if (!SAVE_FILE.exists()) return null;
        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(SAVE_FILE), StandardCharsets.UTF_8)) {
            p.load(r);
            return p;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean hasSave() {
        return SAVE_FILE.exists();
    }

    public static void delete() {
        if (SAVE_FILE.exists()) SAVE_FILE.delete();
    }
}
