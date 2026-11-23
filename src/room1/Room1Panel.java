package room1;

import common.PlaceholderRoom;

/**
 * 1번 방 (팀원 구현 예정).
 * 완성 시 room2.Room2Panel 처럼 common.RoomPanel 을 상속하고
 * getRoomName / getObjective / getCurrentHints 등을 구현하면 된다.
 */
public class Room1Panel extends PlaceholderRoom {
    public Room1Panel() {
        super("1번 방", "[임시] 1번 방 탈출 → 2번 방으로");
    }
}
