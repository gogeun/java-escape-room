package room3;

import static room3.InventoryManager.Item.*;
import room3.puzzles.*;

public final class GameController {
    private final java.util.function.Consumer<String> sound;
    public GameController() { this(SoundManager::play); }
    GameController(java.util.function.Consumer<String> sound) { this.sound=sound; }
    public final GameState state=new GameState();
    public final InventoryManager inventory=new InventoryManager();
    public final CandlePuzzle candles=new CandlePuzzle();
    public final SafePuzzle safe=new SafePuzzle();
    public final CircuitPuzzle circuit=new CircuitPuzzle();
    public boolean obtainRod() {
        if(state.ignitionRodObtained) return false;
        inventory.add(IGNITION_ROD); state.ignitionRodObtained=true; sound.accept("item_get");return true;
    }
    public String light(int index) {
        if(state.candlesSolved) return "불꽃이 고요히 타오른다.";
        if(!state.ignitionRodObtained || !inventory.has(IGNITION_ROD)) return "아무 일도 일어나지 않는다.";
        boolean correct=candles.press(index);
        sound.accept(correct?"candle_light":"candle_fail");
        if(candles.solved()) { state.candlesSolved=true;state.safeUnlocked=true; return "어딘가에서 금속이 움직이는 소리가 들린다."; }
        return correct?"불꽃이 흔들린다.":"불꽃이 일제히 꺼졌다.";
    }
    public String toggleSafe(int index) {
        if(!state.candlesSolved || !state.safeUnlocked) return "아무 반응이 없다.";
        if(state.moonCrystalObtained) return "금고는 이미 열려 있다.";
        int before=safe.count();safe.toggle(index);
        if(safe.count()!=before) sound.accept("safe_click");
        if(safe.solved()) { state.moonCrystalObtained=true; inventory.add(MOON_CRYSTAL);sound.accept("safe_open");sound.accept("item_get");return "낡은 잠금장치가 풀렸다."; }
        return safe.count()==3?"아무 반응이 없다.":"";
    }
    public String combine() {
        if(state.staffCompleted) return "지팡이는 이미 완성되었다.";
        if(!state.ignitionRodObtained || !state.moonCrystalObtained || !inventory.has(IGNITION_ROD) || !inventory.has(MOON_CRYSTAL))
            return "아무 일도 일어나지 않는다.";
        inventory.remove(IGNITION_ROD);inventory.remove(MOON_CRYSTAL);inventory.add(MOON_STAFF);
        state.staffCompleted=true;sound.accept("staff_complete");return "붉은 빛이 지팡이에 스며든다.";
    }
    public boolean canEnterCircuit() { return state.staffCompleted && inventory.has(MOON_STAFF); }
    public boolean rotateCircuit(int index) {
        if(!canEnterCircuit() || state.gameCompleted) return false;
        if(circuit.rotate(index)) sound.accept("circuit_rotate");
        if(circuit.solved()) { state.circuitSolved=true;state.gameCompleted=true;sound.accept("door_open"); }
        return state.gameCompleted;
    }
}
