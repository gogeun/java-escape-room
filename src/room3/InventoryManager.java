package room3;

import java.util.*;

public final class InventoryManager {
    public enum Item {
        IGNITION_ROD("낡은 점화봉", "item_02.png", "손때 묻은 자루에 그을음이 남아 있다."),
        MOON_CRYSTAL("붉은 달의 수정", "item_01.png", "붉은 달의 힘이 깃든 수정."),
        MOON_STAFF("붉은 달의 지팡이", "item_03.png", "붉은 빛이 희미하게 맥동한다.");
        public final String title, image, description;
        Item(String title, String image, String description) {
            this.title=title; this.image=image; this.description=description;
        }
    }
    private final Set<Item> items = new LinkedHashSet<>();
    private Item selected;
    public boolean add(Item item) { return items.size()<8 && items.add(item); }
    public boolean has(Item item) { return items.contains(item); }
    public void remove(Item item) { items.remove(item); if(selected==item) selected=null; }
    public java.util.List<Item> items() { return java.util.List.copyOf(items); }
    public Item selected() { return selected; }
    public void select(Item item) { selected=has(item)?item:null; }
}
