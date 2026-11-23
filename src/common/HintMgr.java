package common;

import mgr.Manager;

import java.util.ArrayList;
import java.util.List;

/**
 * 힌트 매니저 (싱글톤). 강의의 ItemMgr 처럼 Manager&lt;Hint&gt; 를 상속하고
 * 처음 사용할 때 readAll(파일, Hint::new) 로 hints.txt 를 읽는다.
 */
public class HintMgr extends Manager<Hint> {
    private static HintMgr hintMgr = null;

    private HintMgr() {
        readAll(Resources.path("resources/data/hints.txt"), Hint::new);   // Factory<Hint>
    }

    public static HintMgr getInstance() {
        if (hintMgr == null)
            hintMgr = new HintMgr();
        return hintMgr;
    }

    /** 해당 방/단계의 힌트 문장 목록 (없으면 빈 목록) */
    public List<String> getHints(String roomKey, int stage) {
        List<String> result = new ArrayList<>();
        for (Hint h : findAll(roomKey + "-" + stage))
            result.add(h.getText());
        return result;
    }
}
