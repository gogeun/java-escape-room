package common;

import mgr.Manageable;

import java.util.Scanner;

/**
 * 힌트 한 줄. resources/data/hints.txt 에서 HintMgr 가 읽어 온다.
 * 파일 한 줄 형식:  방키 단계번호 힌트내용
 *   예) room2 1 왼쪽 아래 독서대의 [왕위 계승 기록]을 읽어보세요.
 * 같은 방키·단계에 여러 줄이 있으면 위에서부터 1단계 힌트, 2단계 힌트 … 순서가 된다.
 */
public class Hint implements Manageable {
    private String roomKey;
    private int stage;
    private String text;

    public String getRoomKey() { return roomKey; }
    public int getStage()      { return stage; }
    public String getText()    { return text; }

    @Override
    public void read(Scanner scan) {
        roomKey = scan.next();
        stage = scan.nextInt();
        text = scan.nextLine().trim();
    }

    @Override
    public void print() {
        System.out.printf("[%s-%d] %s%n", roomKey, stage, text);
    }

    /** "room2" 처럼 방키만 주거나 "room2-1" 처럼 방키-단계로 검색 */
    @Override
    public boolean matches(String kwd) {
        return kwd.equals(roomKey) || kwd.equals(roomKey + "-" + stage);
    }
}
