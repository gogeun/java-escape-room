package room3;

import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Optional hardware smoke test; explicitly reports unavailable output devices. */
public final class SoundDeviceTest {
    public static void main(String[] args) throws Exception {
        int played=0;
        for(String name:new String[]{"candle_light","candle_fail","safe_click","safe_open","item_get","staff_complete","circuit_rotate","door_open"}) {
            try(AudioInputStream stream=AudioSystem.getAudioInputStream(SoundDeviceTest.class.getResource("/room3/sounds/"+name+".wav"));
                Clip clip=AudioSystem.getClip()) {
                CountDownLatch started=new CountDownLatch(1),stopped=new CountDownLatch(1);
                clip.addLineListener(event->{if(event.getType()==LineEvent.Type.START) started.countDown();if(event.getType()==LineEvent.Type.STOP) stopped.countDown();});
                clip.open(stream);clip.start();
                if(!started.await(3,TimeUnit.SECONDS) || !stopped.await(6,TimeUnit.SECONDS)) throw new AssertionError("Playback timeout: "+name);
                played++;System.out.println("Played: "+name);
            } catch(LineUnavailableException|IllegalArgumentException e) {
                System.out.println("Hardware playback unavailable after "+played+" effects: "+e);return;
            }
        }
        System.out.println("PASS: all 8 WAV clips opened, started and completed on the available output device");
    }
}
