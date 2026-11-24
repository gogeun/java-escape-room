package common;

/**
 * 게임 상태 변화를 통지받는 리스너 (옵저버 패턴).
 * HUD, 인벤토리, 사이드 메뉴 등이 구현해서 화면을 갱신한다.
 * 필요한 메서드만 오버라이드하면 되도록 default 메서드로 제공한다.
 */
public interface GameListener {
    default void onTimeChanged(int remainingSec) {}
    default void onHpChanged(int hp, int maxHp) {}
    default void onHintsChanged(int hintsLeft) {}
    default void onInventoryChanged() {}
    default void onRoomChanged(RoomPanel room, int roomIndex, int roomCount) {}
    default void onProgressChanged() {}
    default void onDamaged() {}
}
