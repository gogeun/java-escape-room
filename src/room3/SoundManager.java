package room3;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.*;

/** A single audio worker owns all clips; no device or file work runs on Swing's EDT. */
public final class SoundManager implements AutoCloseable {
    private static final Set<String> NAMES=Set.of("candle_light","candle_fail","safe_click","safe_open",
        "item_get","staff_complete","circuit_rotate","door_open");
    private static final SoundManager DEFAULT=new SoundManager(SoundManager::load);
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            DEFAULT.close();
            try { DEFAULT.worker.awaitTermination(2,TimeUnit.SECONDS); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
        },"room3-audio-cleanup"));
    }
    @FunctionalInterface interface ClipLoader { Clip load(String name) throws Exception; }
    private final ClipLoader loader;
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(task->{
        Thread thread=new Thread(task,"room3-audio");thread.setDaemon(true);return thread;
    });
    private final Set<String> pending=ConcurrentHashMap.newKeySet();
    private final Set<String> failed=new HashSet<>();
    private final Map<String,Clip> clips=new HashMap<>();
    private final Map<String,ScheduledFuture<?>> releases=new HashMap<>();
    private final AtomicBoolean closed=new AtomicBoolean();

    public SoundManager() { this(SoundManager::load); }
    SoundManager(ClipLoader loader) { this.loader=loader; }
    public static void play(String name) { DEFAULT.playEffect(name); }
    public static void shutdown() { DEFAULT.close(); }

    void playEffect(String name) {
        if(closed.get() || !NAMES.contains(name) || !pending.add(name)) return;
        try {
            worker.execute(()->{
                try {
                    if(closed.get() || failed.contains(name)) return;
                    Clip clip=clips.get(name);
                    if(clip==null) { clip=loader.load(name);clips.put(name,clip); }
                    ScheduledFuture<?> old=releases.remove(name);
                    if(old!=null) old.cancel(false);
                    // Reuse one voice per effect: rapid clicks restart it, never stack copies.
                    clip.stop();clip.setFramePosition(0);clip.start();
                    long delay=Math.max(10_000,clip.getMicrosecondLength()/1000+1000);
                    releases.put(name,worker.schedule(()->release(name),delay,TimeUnit.MILLISECONDS));
                } catch(Exception e) {
                    failed.add(name);release(name);
                    System.err.println("Sound unavailable: "+name+" ("+e.getClass().getSimpleName()+")");
                } finally { pending.remove(name); }
            });
        } catch(RejectedExecutionException e) { pending.remove(name); }
    }
    private static Clip load(String name) throws Exception {
        var resource=SoundManager.class.getResource("/room3/sounds/"+name+".wav");
        if(resource==null) throw new java.io.FileNotFoundException(name+".wav");
        try(AudioInputStream stream=AudioSystem.getAudioInputStream(resource)) {
            Clip clip=AudioSystem.getClip();
            try { clip.open(stream);return clip; }
            catch(Exception e) { clip.close();throw e; }
        }
    }
    private void release(String name) {
        ScheduledFuture<?> timer=releases.remove(name);if(timer!=null) timer.cancel(false);
        Clip clip=clips.remove(name);
        if(clip!=null) { try { clip.stop(); } finally { clip.close(); } }
    }
    @Override public void close() {
        if(!closed.compareAndSet(false,true)) return;
        worker.execute(()->{for(String name:new ArrayList<>(clips.keySet())) {
            try { release(name); } catch(RuntimeException e) { System.err.println("Sound cleanup failed: "+name); }
        }});
        worker.shutdown();
    }
}
