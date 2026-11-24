package room3.puzzles;

import java.util.*;

/** Directions: north=0, east=1, south=2, west=3. Corner starts north/east. */
public final class CircuitPuzzle {
    private final boolean[] corner={false,false,true,true,true,false,true,true,true,false,false,true,true,false,false,true};
    private final boolean[] fixed=new boolean[16];
    // Witness path: 0-1-2-6-5-4-8-9-10-11-15 -> east.
    private final int[] solution={1,1,2,0,1,1,3,0,0,1,1,2,0,0,0,0};
    private final int[] initial=new int[16], rotation=new int[16];
    public CircuitPuzzle() {
        for(int i:new int[]{1,6,8,10}) fixed[i]=true;
        for(int i=0;i<16;i++) initial[i]=(solution[i]+(fixed[i]?0:1))%4;
        reset();
    }
    public boolean corner(int i) { return corner[i]; }
    public boolean fixed(int i) { return fixed[i]; }
    public int rotation(int i) { return rotation[i]; }
    public boolean rotate(int i) { if(i<0 || i>=16 || fixed[i]) return false;rotation[i]=(rotation[i]+1)%4;return true; }
    public void reset() { System.arraycopy(initial,0,rotation,0,16); }
    public boolean connects(int i,int direction) {
        int local=(direction-rotation[i]+4)%4;
        return corner[i]?(local==0 || local==1):(local==0 || local==2);
    }
    public Set<Integer> powered() {
        Set<Integer> seen=new HashSet<>();
        if(!connects(0,3)) return seen;
        ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(0);seen.add(0);
        int[] dr={-1,0,1,0},dc={0,1,0,-1};
        while(!queue.isEmpty()) {
            int i=queue.remove();
            for(int d=0;d<4;d++) {
                int r=i/4+dr[d], c=i%4+dc[d];
                if(r<0 || r>3 || c<0 || c>3) continue;
                int next=r*4+c;
                if(connects(i,d) && connects(next,(d+2)%4) && seen.add(next)) queue.add(next);
            }
        }
        return seen;
    }
    public boolean solved() { return connects(15,1) && powered().contains(15); }
}
