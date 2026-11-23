package common;

import mgr.Manageable;
import mgr.Manager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;

/**
 * 탈출 성공 기록(랭킹)을 data/ranking.txt 에 저장하고 불러온다.
 * 한 줄 형식: 이름\t난이도\t점수\t클리어시간(초)\t등급\t날짜
 *
 * 불러올 때는 강의 구조대로 Manager&lt;Entry&gt;.readAll(파일, Entry::new) 를 사용한다.
 */
public final class RankingManager {
    private RankingManager() {}

    private static final File FILE = Resources.file("data/ranking.txt");
    public static final int MAX_SHOW = 10;

    /** 랭킹 기록 한 줄 (Manageable 구현) */
    public static class Entry implements Manageable {
        public String name;
        public Difficulty difficulty;
        public int score;
        public int clearSec;
        public String grade;
        public String date;

        /** Factory(Entry::new) 용 빈 생성자 */
        public Entry() {}

        Entry(String name, Difficulty difficulty, int score, int clearSec, String grade, String date) {
            this.name = name;
            this.difficulty = difficulty;
            this.score = score;
            this.clearSec = clearSec;
            this.grade = grade;
            this.date = date;
        }

        String toLine() {
            return String.join("\t", name.replace("\t", " "), difficulty.name(),
                String.valueOf(score), String.valueOf(clearSec), grade, date);
        }

        /** 줄이 깨져 있으면 difficulty 가 null 로 남는다 */
        boolean isValid() {
            return difficulty != null;
        }

        @Override
        public void read(Scanner scan) {
            String[] t = scan.nextLine().split("\t");
            if (t.length < 6) return;
            try {
                name = t[0];
                score = Integer.parseInt(t[2]);
                clearSec = Integer.parseInt(t[3]);
                grade = t[4];
                date = t[5];
                difficulty = Difficulty.valueOf(t[1]);
            } catch (Exception e) {
                difficulty = null;
            }
        }

        @Override
        public void print() {
            System.out.printf("%s (%s) %d점 %s %s%n", name, difficulty, score, grade, date);
        }

        /** 이름 또는 난이도(EASY/NORMAL/HARD)로 검색 */
        @Override
        public boolean matches(String kwd) {
            return kwd.equals(name) || (difficulty != null && kwd.equals(difficulty.name()));
        }
    }

    public static void addEntry(GameResult r) {
        Entry e = new Entry(r.getPlayerName(), r.getDifficulty(), r.getScore(),
            r.getElapsedSec(), r.getGrade(), LocalDate.now().toString());
        FILE.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(FILE, true), StandardCharsets.UTF_8)) {
            w.write(e.toLine() + System.lineSeparator());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /** 파일 전체를 Manager 로 읽어 온다 */
    private static Manager<Entry> loadManager() {
        Manager<Entry> mgr = new Manager<>();
        if (FILE.exists()) {
            mgr.readAll(FILE.getPath(), Entry::new);   // Factory<Entry>
            mgr.mList.removeIf(e -> !e.isValid());
        }
        return mgr;
    }

    /** 점수 높은 순, 같으면 빨리 깬 순으로 정렬 */
    public static List<Entry> loadSorted() {
        return sort(new ArrayList<>(loadManager().mList));
    }

    /** 키워드(이름 또는 난이도 이름)에 맞는 기록만 정렬해서 반환 (Manager.findAll 사용) */
    public static List<Entry> search(String kwd) {
        return sort(loadManager().findAll(kwd));
    }

    private static List<Entry> sort(List<Entry> list) {
        list.sort(Comparator.comparingInt((Entry e) -> -e.score).thenComparingInt(e -> e.clearSec));
        return list;
    }

    public static void clear() {
        if (FILE.exists()) FILE.delete();
    }
}
