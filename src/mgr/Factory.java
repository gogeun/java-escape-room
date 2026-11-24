package mgr;

/**
 * Manager.readAll() 이 파일을 읽을 때 빈 객체를 하나씩 만들어 주는 팩토리.
 * 보통 생성자 참조로 넘긴다. 예) hintMgr.readAll(path, Hint::new);
 */
public interface Factory<T extends Manageable> {
    T create();
}
