package mgr;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Manageable 객체들을 리스트로 관리하는 제네릭 매니저 (강의 StoreManager 구조와 동일).
 *
 * 이 프로젝트에서의 사용:
 *  - GameManager      : Manager<Item>  → 인벤토리
 *  - HintMgr          : Manager<Hint>  → resources/data/hints.txt 의 힌트
 *  - RankingManager   : Manager<Entry> → data/ranking.txt 의 탈출 기록
 *
 * 강의 코드와 다른 점: 파일이 없을 때 프로그램을 종료(System.exit)하지 않고
 * 빈 목록으로 진행한다. (게임 도중 꺼지면 안 되기 때문)
 */
public class Manager<T extends Manageable> {
    public List<T> mList = new ArrayList<>();

    public T find(String kwd) {
        for (T m : mList)
            if (m.matches(kwd))
                return m;
        return null;
    }

    public void readAll(String filename, Factory<T> fac) {
        Scanner filein = openFile(filename);
        if (filein == null) return;
        T m = null;
        while (filein.hasNext()) {
            m = fac.create();
            m.read(filein);
            mList.add(m);
        }
        filein.close();
    }

    public void printAll() {
        for (T m : mList) {
            m.print();
        }
    }

    public List<T> findAll(String kwd) {
        List<T> result = new ArrayList<>();
        for (T m : mList) {
            if (m.matches(kwd))
                result.add(m);
        }
        return result;
    }

    public Scanner openFile(String filename) {
        Scanner filein = null;
        try {
            filein = new Scanner(new File(filename), "UTF-8");
        } catch (Exception e) {
            System.out.println(filename + ": 파일 없음");
        }
        return filein;
    }

    public void addElement(T e) {
        mList.add(e);
    }

    public boolean removeElement(String kwd) {
        return mList.removeIf(m -> m.matches(kwd));
    }

    public int size() {
        return mList.size();
    }

    public void clear() {
        mList.clear();
    }
}
