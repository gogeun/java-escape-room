package room3;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Dispatch mouse events through the complete Swing game and modal dialogs. */
public final class GuiTest {
    private static JFrame frame;
    private static Room3Panel panel;
    private static JPanel host;
    private static CardLayout rooms;
    private static final java.util.concurrent.atomic.AtomicInteger completed=new java.util.concurrent.atomic.AtomicInteger();
    private static void settle() throws Exception {Thread.sleep(300);SwingUtilities.invokeAndWait(()->{});}
    private static <T extends Component> T find(Container parent,Class<T> type,java.util.function.Predicate<T> test) {
        for(Component child:parent.getComponents()) {
            if(type.isInstance(child)&&test.test(type.cast(child))) return type.cast(child);
            if(child instanceof Container c) {T result=find(c,type,test);if(result!=null) return result;}
        }return null;
    }
    private static Window active() {for(Window w:Window.getWindows()) if(w instanceof JDialog&&w.isShowing()) return w;return frame;}
    private static void click(Component c) throws Exception {
        if(c==null) throw new AssertionError("Missing control");
        SwingUtilities.invokeLater(()->((JButton)c).doClick());settle();
    }
    private static void button(String text) throws Exception {click(find(active(),JButton.class,b->text.equals(b.getAccessibleContext().getAccessibleName())));}
    private static void check(boolean condition,String message) {if(!condition) throw new AssertionError(message);}
    private static boolean hotspot(String title) {return find(active(),JButton.class,b->title.equals(b.getAccessibleContext().getAccessibleName()))!=null;}
    private static GameController game() throws Exception {
        var field=Room3Panel.class.getDeclaredField("game");field.setAccessible(true);return (GameController)field.get(panel);
    }
    private static void noInstructions() {
        if(!TextDisplay.ENABLED) {
            check(find(active(),JLabel.class,label->label.getText()!=null && !label.getText().isEmpty())==null,"Visible label text remains");
            check(find(active(),JButton.class,b->b.getText()!=null && !b.getText().isEmpty())==null,"Visible button text remains");
            check(find(active(),JComponent.class,c->c.getToolTipText()!=null)==null,"Visible tooltip remains");
        }
        String[] forbidden={"비교하자","대응시키자","선택하자","기억하자","조합하자","금색 테두리","회전할","입구 →","점화봉 +","다음 촛불","조사하세요"};
        check(find(active(),JLabel.class,label->{for(String word:forbidden) if(label.getText()!=null && label.getText().contains(word)) return true;return false;})==null,"Visible puzzle instructions remain");
        check(find(active(),JButton.class,b->"조사 도움말".equals(b.getText()) || "두 재료 조합하기".equals(b.getText()))==null,"Tutorial or recipe button remains");
    }
    private static void scene(double x,double y,double ratio) throws Exception {
        Component c=find(active(),JPanel.class,p->p.getClass().getName().endsWith("$Scene"));
        if(c==null) throw new AssertionError("Missing scene");
        SwingUtilities.invokeLater(()->{
            double w=c.getWidth(),h=w/ratio;if(h>c.getHeight()) {h=c.getHeight();w=h*ratio;}
            int px=(int)((c.getWidth()-w)/2+x*w),py=(int)((c.getHeight()-h)/2+y*h);
            Component target=((Container)c).findComponentAt(px,py);
            if(target instanceof JButton b) b.doClick();
            else throw new AssertionError("No transparent button at "+x+", "+y);
        });settle();
    }
    private static void snapshot(String name) throws Exception {
        SwingUtilities.invokeAndWait(()->{try {Container c=active();BufferedImage im=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=im.createGraphics();c.printAll(g);g.dispose();ImageIO.write(im,"png",new File("out/"+name+".png"));} catch(Exception e) {throw new RuntimeException(e);}});
    }
    public static void main(String[] args) throws Exception {
        try {
            SwingUtilities.invokeAndWait(()->{
                frame=new JFrame("Bloodmoon GUI test");rooms=new CardLayout();host=new JPanel(rooms);
                panel=new Room3Panel(()->{completed.incrementAndGet();rooms.show(host,"next");});
                host.add(panel,"room3");host.add(new JPanel(),"next");
                frame.setContentPane(host);frame.setSize(1100,700);frame.setLocationRelativeTo(null);frame.setVisible(true);
            });settle();
            snapshot("chapel-preview");
            noInstructions();
            check(!hotspot("의자 아래 숨겨진 금고"),"Safe must be inactive before candles");
            check(!hotspot("장식장 · 숨겨진 금고"),"Wardrobe must not lead to safe");
            scene(.56,.56,1672.0/941);
            check(hotspot("제단") && !hotspot("책상 위 다섯 촛불"),"Altar is for item use only");
            scene(.50,.65,1670.0/942);check(!game().state.staffCompleted,"Empty altar use must not combine");noInstructions();button("← 예배당으로 돌아가기");
            scene(.12,.75,1672.0/941);snapshot("desk-preview");
            check(hotspot("책상 서랍") && hotspot("책상 위 다섯 촛불"),"Desk must provide drawer and candles");
            check(!hotspot("제단"),"No combination at desk");
            scene(.50,.83,1670.0/941); // open drawer, obtain rod
            scene(.45,.45,1670.0/941);check(active()!=frame,"Book popup opens inside host window");
            SwingUtilities.invokeAndWait(()->rooms.show(host,"next"));settle();
            check(active()==frame,"Hiding a room closes its modal popup");
            var audioField=Room3Panel.class.getDeclaredField("audio");audioField.setAccessible(true);
            check(audioField.get(panel)==null,"Hidden room releases its own audio");
            SwingUtilities.invokeAndWait(()->rooms.show(host,"room3"));settle();
            check(game().state.ignitionRodObtained && audioField.get(panel)!=null,"Re-entry retains progress and reopens audio");
            SwingUtilities.invokeAndWait(()->panel.setSoundEnabled(false));
            check(!panel.isSoundEnabled() && audioField.get(panel)==null,"Host can mute room audio");
            SwingUtilities.invokeAndWait(()->panel.setSoundEnabled(true));
            scene(.75,.65,1670.0/941);button("다음 →");noInstructions();snapshot("clue-preview");button("닫기");
            scene(.80,.30,1670.0/941);noInstructions();snapshot("potion-preview");button("닫기");
            SwingUtilities.invokeAndWait(()->frame.setSize(960,640));settle();
            scene(.22,.25,1670.0/941);
            noInstructions();
            double[] xs={.20,.355,.509,.665,.821},ys={.31,.49,.35,.22,.07};
            for(int n:new int[]{2,3,5,1,4}) scene(xs[n-1],ys[n-1]+.1,2);
            snapshot("candle-preview");button("닫기");button("← 예배당으로 돌아가기");
            check(hotspot("의자 아래 숨겨진 금고"),"Safe appears after candles");
            scene(.94,.86,1672.0/941);
            noInstructions();
            for(String pos:new String[]{"1행 3열","2행 1열","3행 2열"}) button(pos);
            button("닫기");scene(.56,.56,1672.0/941);snapshot("altar-preview");
            scene(.50,.65,1670.0/942);check(!game().state.staffCompleted,"Items must be selected for altar use");
            button("낡은 점화봉");scene(.50,.65,1670.0/942);check(game().state.staffCompleted,"Selected item use completes staff");noInstructions();
            button("← 예배당으로 돌아가기");scene(.91,.40,1672.0/941);snapshot("circuit-preview");
            noInstructions();
            for(int i=0;i<16;i++) if(i!=1&&i!=6&&i!=8&&i!=10) for(int turn=0;turn<3;turn++) {
                if(active()==frame) break;
                scene(.20+(i%4)*.20,.24+(i/4)*.195,956.0/994);
            }
            snapshot("escape-preview");
            if(!game().state.gameCompleted) throw new AssertionError("GUI did not reach ending");
            check(panel.isCompleted() && completed.get()==1 && !panel.isShowing(),"Host completion callback transitions once");
            SwingUtilities.invokeAndWait(()->rooms.show(host,"room3"));settle();
            check(panel.isCompleted() && completed.get()==1,"Re-entry does not repeat completion callback");
            check(active()==frame,"No orphan room dialogs after transition");
            System.out.println("PASS: full playthrough, host callback, CardLayout re-entry, mute and audio cleanup");
        } finally {SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows()) w.dispose();});}
    }
}
