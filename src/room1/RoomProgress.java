package room1;

import java.util.EnumSet;

/** Inventory and mechanical state survive closing a panel or restarting its shutdown sequence. */
final class RoomProgress {
    enum Item {
        HANDLE("밸브 손잡이", "공구", "황동 밸브 손잡이",
                "다섯 개의 살이 달린 묵직한 황동 바퀴다.", "가운데 압력 기관의 빈 축에 꼭 맞는다."),
        CHART("압력 정비표", "정비표", "항공기관 표준 압력",
                "착륙 전 안전 압력은 50. 눈금이 50 근처에 오면 손잡이를 놓아 고정한다.",
                "시계 방향으로 돌리면 압력이 내려가고, 반대로 돌리면 다시 오른다."),
        TAG("정비 표찰", "정비 기록", "기관 정지 절차",
                "연료가 끊겨야 냉각수가 흐른다. 냉각 직후 증기를 빼고, 마지막에 시간축을 잠근다.",
                "연료 레버 ↓     냉각 스위치 ↑     배출 고리 ↓     잠금 걸쇠 →"),
        TOKEN("승선 기념패", "기념품", "공중도시 운항 기념",
                "닳은 황동 표면에 공중도시의 윤곽이 새겨져 있다.", "기관사가 남긴 작은 기념품이다.");

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
        DESK("정비 작업대"), PRESSURE("압력 기관"), STOP("기관 정지 패널"),
        LAMP("점검등"), WINDOW("전망창"), TUBE("전성관"), LIFT("시간 승강기");

        final String name;
        Station(String name) { this.name = name; }
    }

    enum Command {
        FUEL("연료 차단", "아래로 내리기"),
        COOL("냉각", "위로 올리기"),
        VENT("증기 배출", "아래로 당기기"),
        AXIS("시간축 잠금", "오른쪽 밀기");

        final String label, gesture;
        Command(String label, String gesture) { this.label = label; this.gesture = gesture; }
    }

    enum Response { INACTIVE, ENGAGED, MISTAKE, RESTARTED, FINISHED }

    private static final Command[] PROCEDURE = {Command.FUEL, Command.COOL, Command.VENT, Command.AXIS};
    private static final double TARGET_TURNS = 2.0;
    private final EnumSet<Item> inventory = EnumSet.noneOf(Item.class);
    private boolean handleInstalled;
    private int solved, step, hearts = 3;
    private double turns;

    boolean take(Item item) { return inventory.add(item); }
    boolean has(Item item) { return inventory.contains(item); }
    int count() { return inventory.size(); }
    int solved() { return solved; }
    int step() { return step; }
    int hearts() { return hearts; }
    double turns() { return turns; }
    double pressure() { return 85 - 35 * turns / TARGET_TURNS; }
    boolean installed(Item item) { return item == Item.HANDLE && handleInstalled; }

    boolean install(Station station) {
        if (station != Station.PRESSURE || !has(Item.HANDLE) || handleInstalled) return false;
        handleInstalled = true;
        return true;
    }

    boolean ready(Station station) {
        if (station == Station.PRESSURE) return solved == 0 && handleInstalled;
        return station == Station.STOP && solved == 1 && has(Item.TAG);
    }

    void turn(double deltaRadians) {
        if (ready(Station.PRESSURE)) {
            turns = Math.max(0, Math.min(3, turns + deltaRadians / (2 * Math.PI)));
        }
    }

    boolean releaseValve() {
        if (!ready(Station.PRESSURE) || Math.abs(pressure() - 50) > 1.5) return false;
        turns = TARGET_TURNS;
        solved = 1;
        return true;
    }

    boolean engaged(Command command) {
        for (int i = 0; i < step; i++) {
            if (PROCEDURE[i] == command) return true;
        }
        return false;
    }

    Response operate(Command command) {
        if (!ready(Station.STOP) || engaged(command)) return Response.INACTIVE;
        if (command != PROCEDURE[step]) {
            step = 0;
            if (--hearts > 0) return Response.MISTAKE;
            hearts = 3;
            return Response.RESTARTED;
        }
        if (++step < PROCEDURE.length) return Response.ENGAGED;
        solved = 2;
        return Response.FINISHED;
    }
}
