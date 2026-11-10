package room3;

import room3.puzzles.*;
import static room3.InventoryManager.Item.*;

/** Standard-library regression tests; throws on failure without relying on -ea. */
public final class GameTest {
    private static int checks;
    private static void check(boolean value,String message) { checks++;if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        GameController g=new GameController();
        check(!g.state.safeUnlocked && !g.state.moonCrystalObtained,"Safe and crystal initially unavailable");
        g.light(2);check(!g.candles.lit(2),"Cannot light without rod");
        g.toggleSafe(2);check(g.safe.count()==0,"Safe gated by candles");
        g.combine();check(!g.state.staffCompleted,"Combination gated");
        check(!g.canEnterCircuit(),"Circuit gated");
        check(g.obtainRod(),"Obtain rod");check(!g.obtainRod(),"No duplicate rod");
        g.light(2);g.light(1);check(!g.candles.lit(2),"Wrong candle resets all");
        for(int n:new int[]{2,3,5,1,4}) g.light(n);
        check(g.state.candlesSolved && g.state.safeUnlocked,"Candles unlock hidden safe");
        g.light(1);check(g.state.candlesSolved,"Solved candles persist");
        for(int n:new int[]{0,1,2,3}) g.toggleSafe(n);
        check(g.safe.count()==3,"Selection cap");check(!g.state.moonCrystalObtained,"Wrong triple rejected");
        g.toggleSafe(0);g.toggleSafe(1);g.toggleSafe(7);g.toggleSafe(3);
        check(g.state.moonCrystalObtained && g.inventory.has(MOON_CRYSTAL),"Safe correct independent of order");
        g.toggleSafe(0);check(g.inventory.items().size()==2,"No duplicate crystal");
        g.combine();check(g.state.staffCompleted && g.inventory.has(MOON_STAFF),"Combine succeeds");
        check(!g.inventory.has(IGNITION_ROD) && !g.inventory.has(MOON_CRYSTAL),"Ingredients consumed");
        g.combine();check(g.inventory.items().size()==1,"No duplicate staff");
        check(g.canEnterCircuit(),"Completed staff activates circuit without slot selection");
        check(g.state.ignitionRodObtained && g.state.moonCrystalObtained,"Acquisition history survives ingredient consumption");
        CircuitPuzzle c=g.circuit;check(!c.solved(),"Initial board unsolved");
        int fixed=0;for(int i=0;i<16;i++) if(c.fixed(i)) {fixed++;int before=c.rotation(i);check(!c.rotate(i)&&before==c.rotation(i),"Fixed tile immutable");}
        check(fixed==4,"Exactly four fixed tiles");
        c.rotate(0);c.reset();check(c.rotation(0)==2,"Reset restores initial board");
        // A designed witness solution, independent of BFS implementation.
        int[] solution={1,1,2,0,1,1,3,0,0,1,1,2,0,0,0,0};
        for(int i=0;i<16;i++) while(c.rotation(i)!=solution[i]) c.rotate(i);
        check(c.solved(),"Witness route connects both terminals");
        check(c.powered().size()<16,"All cells not required");
        c.reset();
        for(int i=0;i<16 && !g.state.gameCompleted;i++) {
            int remaining=4;while(c.rotation(i)!=solution[i] && remaining-->0 && !g.state.gameCompleted) g.rotateCircuit(i);
        }
        check(g.state.circuitSolved&&g.state.gameCompleted,"Full game reaches escape");
        String[] names={"background.png","desk.png","altar.png","vault.png","candle.png","potion.png","circuit_board.png","tile_straight.png","tile_corner.png","tile_fixed_straight.png","tile_fixed_corner.png","item_01.png","item_02.png","item_03.png","pop-up frame.png","clue_00.png","clue_01.png","clue_02.png","clue_03.png","clue_04.png","clue_05.png"};
        AssetManager assets=new AssetManager();for(String n:names) check(assets.image(n).getWidth()>0,"Load "+n);
        check(assets.missing().isEmpty(),"All packaged assets available");
        AssetManager missing=new AssetManager();check(missing.image("missing.png")!=null&&!missing.missing().isEmpty(),"Missing image recovers");
        javax.swing.SwingUtilities.invokeAndWait(()->{
            Room3Panel panel=new Room3Panel();panel.setSize(1280,800);panel.doLayout();
            var im=new java.awt.image.BufferedImage(1280,800,java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics=im.createGraphics();panel.printAll(graphics);graphics.dispose();
        });
        System.out.println("PASS: "+checks+" checks, full progression and Swing construction");
    }
}
