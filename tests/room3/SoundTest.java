package room3;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sound.sampled.*;
import javax.swing.SwingUtilities;

/** Tests event transitions independently of hardware, then tests the asynchronous player. */
public final class SoundTest {
    private static int checks;
    private static void check(boolean value,String message) {checks++;if(!value) throw new AssertionError(message);}
    private static long count(List<String> events,String name) {return events.stream().filter(name::equals).count();}
    private static void await(CountDownLatch latch,String name) throws Exception {check(latch.await(3,TimeUnit.SECONDS),name);}
    public static void main(String[] args) throws Exception {
        List<String> events=new ArrayList<>();GameController g=new GameController(events::add);
        g.light(2);g.toggleSafe(2);g.combine();g.rotateCircuit(0);
        check(events.isEmpty(),"Locked actions are silent");
        g.obtainRod();g.obtainRod();check(count(events,"item_get")==1,"Rod first acquisition only");
        g.light(2);g.light(1);
        check(events.subList(1,3).equals(List.of("candle_light","candle_fail")),"Correct and wrong candle sounds");
        for(int i:new int[]{2,3,5,1,4}) g.light(i);
        g.light(2);check(count(events,"candle_light")==6 && count(events,"candle_fail")==1,"Every correct flame, no solved replay");
        for(int i:new int[]{0,1,2,3}) g.toggleSafe(i);
        check(count(events,"safe_click")==3,"Ignored fourth selection is silent");
        for(int i:new int[]{0,1,7,3}) g.toggleSafe(i);
        g.toggleSafe(0);
        check(count(events,"safe_click")==7,"Safe selection and deselection clicks");
        check(count(events,"safe_open")==1 && count(events,"item_get")==2,"First safe success and crystal only");
        g.combine();g.combine();check(count(events,"staff_complete")==1,"First combination only");
        g.rotateCircuit(1);check(count(events,"circuit_rotate")==0,"Fixed tile silent");
        int[] solution={1,1,2,0,1,1,3,0,0,1,1,2,0,0,0,0};int rotations=0;
        for(int i=0;i<16 && !g.state.gameCompleted;i++) while(g.circuit.rotation(i)!=solution[i] && !g.state.gameCompleted) {
            g.rotateCircuit(i);rotations++;
        }
        g.rotateCircuit(0);
        check(count(events,"circuit_rotate")==rotations,"Exactly one sound per actual rotation");
        check(g.state.gameCompleted && count(events,"door_open")==1,"Door opens once");

        String[] names={"candle_light","candle_fail","safe_click","safe_open","item_get","staff_complete","circuit_rotate","door_open"};
        for(String name:names) {
            var url=SoundTest.class.getResource("/room3/sounds/"+name+".wav");check(url!=null,"Packaged WAV "+name);
            try(AudioInputStream stream=AudioSystem.getAudioInputStream(url)) {
                check(stream.getFrameLength()>0 && stream.getFormat().getSampleRate()>0,"Valid WAV "+name);
                System.out.println(name+": "+stream.getFormat());
            }
        }
        CountDownLatch loading=new CountDownLatch(1),allowLoad=new CountDownLatch(1),firstStart=new CountDownLatch(1),secondStart=new CountDownLatch(1),released=new CountDownLatch(1);
        AtomicInteger loads=new AtomicInteger(),starts=new AtomicInteger(),closes=new AtomicInteger();AtomicBoolean onEdt=new AtomicBoolean();
        Clip clip=(Clip)Proxy.newProxyInstance(SoundTest.class.getClassLoader(),new Class<?>[]{Clip.class},(proxy,method,values)->{
            onEdt.compareAndSet(false,SwingUtilities.isEventDispatchThread());
            switch(method.getName()) {
                case "start" -> {if(starts.incrementAndGet()==1) firstStart.countDown();else secondStart.countDown();}
                case "close" -> {closes.incrementAndGet();released.countDown();}
                case "getMicrosecondLength" -> {return 100_000L;}
            }
            Class<?> type=method.getReturnType();
            if(type==boolean.class) return false;if(type==int.class) return 0;if(type==long.class) return 0L;if(type==float.class) return 0f;return null;
        });
        SoundManager player=new SoundManager(name->{loads.incrementAndGet();loading.countDown();allowLoad.await();return clip;});
        try {
            SwingUtilities.invokeAndWait(()->player.playEffect("safe_click"));await(loading,"Audio load starts in worker");
            SwingUtilities.invokeAndWait(()->{});check(!onEdt.get(),"EDT remains responsive during audio load");
            player.playEffect("safe_click"); // coalesce duplicate queued request
            allowLoad.countDown();await(firstStart,"First playback");
            Thread.sleep(30);player.playEffect("safe_click");await(secondStart,"Repeated short playback");
            check(loads.get()==1 && starts.get()==2 && !onEdt.get(),"One cached clip, replay off EDT");
        } finally {allowLoad.countDown();player.close();}
        await(released,"Clip released on close");check(closes.get()==1,"Clip closed once");
        AtomicInteger attempts=new AtomicInteger();CountDownLatch missing=new CountDownLatch(1);
        try(SoundManager unavailable=new SoundManager(name->{attempts.incrementAndGet();missing.countDown();throw new LineUnavailableException("test device unavailable");})) {
            unavailable.playEffect("door_open");await(missing,"Missing device handled asynchronously");Thread.sleep(30);
            unavailable.playEffect("door_open");unavailable.playEffect("unknown");Thread.sleep(30);
            check(attempts.get()==1,"Failed sound disabled without retry storm");
        }
        System.out.println("PASS: "+checks+" audio checks (mapping, WAVs, EDT, replay, cleanup and device failure)");
    }
}
