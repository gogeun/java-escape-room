package room3.puzzles;

import java.util.*;
public final class SafePuzzle {
    private final Set<Integer> selected=new HashSet<>();
    public void toggle(int index) {
        if(index<0 || index>8 || solved()) return;
        if(!selected.remove(index) && selected.size()<3) selected.add(index);
    }
    public boolean selected(int index) { return selected.contains(index); }
    public int count() { return selected.size(); }
    public boolean solved() { return selected.equals(Set.of(2,3,7)); }
}
