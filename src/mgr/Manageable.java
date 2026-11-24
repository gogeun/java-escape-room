package mgr;

import java.util.Scanner;

/**
 * Manager 가 관리할 수 있는 객체의 공통 인터페이스 (강의 StoreManager 구조와 동일).
 *  - read(scan)   : 파일 한 항목을 읽어 자기 필드를 채운다
 *  - print()      : 콘솔에 내용 출력
 *  - matches(kwd) : 검색 키워드와 일치하는지
 *
 * 이 프로젝트에서 구현한 클래스: common.Item, common.Hint, common.RankingManager.Entry
 */
public interface Manageable {
    void read(Scanner scan);
    void print();
    boolean matches(String kwd);
}
