package common;

import mgr.Manageable;

import java.awt.*;
import java.util.Scanner;

/**
 * 인벤토리에 들어가는 아이템 (단서/열쇠 등).
 * 각 방에서 new Item(...) 으로 만들어 GameManager.addItem() 으로 등록한다.
 * onView 에는 아이템을 클릭했을 때 열릴 확대보기 창을 넣는다.
 *
 * Manageable 을 구현하므로 GameManager 의 인벤토리(Manager&lt;Item&gt;)에서
 * find / findAll 로 검색할 수 있다.
 */
public class Item implements Manageable {
    private String id;
    private String name;
    private String description;
    private Image icon;       // null 가능
    private Runnable onView;  // null 가능

    /** Factory(Item::new) 로 파일에서 읽을 때 사용하는 빈 생성자 */
    public Item() {}

    public Item(String id, String name, String description, Image icon, Runnable onView) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.onView = onView;
    }

    public String getId()          { return id; }
    public String getName()        { return name; }
    public String getDescription() { return description; }
    public Image getIcon()         { return icon; }

    public boolean isViewable() { return onView != null; }

    public void view() {
        if (onView != null) onView.run();
    }

    // ---------------- Manageable 구현 ----------------

    /** 형식: 아이디 이름(공백은 _)  다음 줄에 설명.   예) I07 왕실_인증패 \n 초상화 장식장에서 얻은 인증패. */
    @Override
    public void read(Scanner scan) {
        id = scan.next();
        name = scan.next().replace('_', ' ');
        scan.nextLine();
        description = scan.hasNextLine() ? scan.nextLine().trim() : "";
    }

    @Override
    public void print() {
        System.out.printf("[%s] %s - %s%n", id, name, description);
    }

    /** 아이디가 같거나 이름에 키워드가 들어 있으면 일치 */
    @Override
    public boolean matches(String kwd) {
        return id.equals(kwd) || name.contains(kwd);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Item && ((Item) o).id.equals(id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}
