package room1;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

/** 1번 방의 진행 상태. 도구를 장착해도 설명서와 꼬리표는 계속 읽을 수 있다. */
final class RoomProgress {
    enum Item {
        I01("정비 문서", "정비 일지", "톱니 회전 수", "1차   1530     /     2차   1560     /     3차   1590", "4차 기록을 정비함의 암호로 사용할 것."),
        I02("압력 보정 키트", "키트 설명서", "밸브 보정 규격", "안전 압력은 50이다. 밸브를 한 번 돌릴 때마다 압력이 5씩 내려간다.", "압력 제어판의 결합부에 키트를 장착한다."),
        I03("정지 패널 해제키", "열쇠 꼬리표", "정지 패널 정비용", "시간축은 반드시 마지막에 잠가라.", "황동 열쇠를 패널 옆 열쇠 구멍에 끼운다."),
        I04("비상 운행 카드", "승강기 운행증", "시간 기관부 발행", "시간 기관을 정지한 뒤 승강기 옆 판독기에 삽입한다.", "운행 좌표는 실제 기관 정지 시각으로 설정한다."),
        I05("기관 정지 기록", "정지 기록지", "중앙 시계 검침", "중앙 시계가 10시 20분에 멈췄다.", "검침 메모   이 시계는 25분 빠르다.");

        final String name, form, heading, text, note;

        Item(String name, String form, String heading, String text, String note) {
            this.name = name;
            this.form = form;
            this.heading = heading;
            this.text = text;
            this.note = note;
        }
    }

    enum Station {
        DESK("정비 책상", null),
        LOCKER("정비함", null,
                "정비 책상의 문서를 펼쳐 보세요. 정비함에 새겨진 ‘4차’는 그 문서의 다음 기록을 뜻합니다.",
                "1차에서 2차, 2차에서 3차로 갈 때 회전 수가 얼마나 늘었나요? 3차 기록에 그만큼 더해 보세요.",
                "1590에 30을 더한 1620을 입력하세요."),
        PRESSURE("압력 제어판", Item.I02,
                "제어판에 압력 보정 키트를 장착하고, 키트 설명서에서 안전 압력을 찾아보세요.",
                "현재 압력 85에서 안전 압력 50을 빼 보세요. 그 차이를 한 번 돌릴 때 내려가는 압력 5로 나누면 됩니다.",
                "밸브를 돌릴 횟수에 7을 입력하세요."),
        STOP("정지 패널", Item.I03,
                "해제키를 돌린 뒤 꼬리표를 읽어 보세요. 마지막에 눌러야 할 버튼을 먼저 알아둘 수 있습니다.",
                "냉각 전에 연료를 끊어야 하고, 냉각 직후에는 증기를 빼야 합니다. 빨간 비상 정지 버튼은 고장 났습니다.",
                "연료 차단 → 냉각 → 증기 배출 → 시간축 잠금 순서로 누르세요."),
        LIFT("승강기", Item.I04,
                "비상 운행 카드를 삽입한 뒤, 소지품에서 함께 받은 기관 정지 기록을 읽어 보세요.",
                "기록된 10시 20분은 실제보다 25분 빠릅니다. 20분을 먼저 빼고, 남은 5분도 거슬러 가세요.",
                "실제 정지 시각은 9시 55분입니다. 시간 좌표에 0955를 입력하세요.");

        final String name;
        final Item tool;
        final String[] hints;

        Station(String name, Item tool, String... hints) {
            this.name = name;
            this.tool = tool;
            this.hints = hints;
        }
    }

    enum Command {
        FUEL("연료 차단"), COOL("냉각"), VENT("증기 배출"), AXIS("시간축 잠금"), EMERGENCY("비상 정지");
        final String label;
        Command(String label) { this.label = label; }
    }

    private static final Command[] PROCEDURE = {Command.FUEL, Command.COOL, Command.VENT, Command.AXIS};
    static final List<Station> PUZZLES = List.of(Station.LOCKER, Station.PRESSURE, Station.STOP, Station.LIFT);
    private final EnumMap<Station, Integer> hintLevels = new EnumMap<>(Station.class);
    private final EnumSet<Item> inventory = EnumSet.noneOf(Item.class);
    private final EnumSet<Item> installed = EnumSet.noneOf(Item.class);
    private int solved, step;

    boolean has(Item item) { return inventory.contains(item); }
    boolean installed(Item item) { return installed.contains(item); }
    int count() { return inventory.size(); }
    int solved() { return solved; }
    int step() { return step; }

    int hintLevel(Station puzzle) { return hintLevels.getOrDefault(puzzle, 0); }

    void revealHint(Station puzzle) {
        int level = hintLevel(puzzle);
        if (level < puzzle.hints.length) hintLevels.put(puzzle, level + 1);
    }

    int hintsUsed() {
        int total = 0;
        for (int count : hintLevels.values()) total += count;
        return total;
    }

    boolean takeDocument() { return inventory.add(Item.I01); }

    boolean install(Station station) {
        return station.tool != null && has(station.tool) && installed.add(station.tool);
    }

    boolean submit(Station station, String input) {
        switch (station) {
            case LOCKER:
                if (solved != 0 || !input.equals("1620")) return false;
                inventory.add(Item.I02);
                break;
            case PRESSURE:
                if (solved != 1 || !installed(Item.I02) || !input.equals("7")) return false;
                inventory.add(Item.I03);
                break;
            case LIFT:
                if (solved != 3 || !installed(Item.I04) || !input.equals("0955")) return false;
                break;
            default:
                return false;
        }
        solved++;
        return true;
    }

    boolean press(Command command) {
        if (solved != 2 || !installed(Item.I03)) return false;
        if (command != PROCEDURE[step]) {
            step = 0;
            return false;
        }
        if (++step == PROCEDURE.length) {
            inventory.add(Item.I04);
            inventory.add(Item.I05);
            solved++;
        }
        return true;
    }
}
