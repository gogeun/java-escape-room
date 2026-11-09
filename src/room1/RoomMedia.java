package room1;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

final class RoomMedia {
    private RoomMedia() {}

    private static URL resource(String name) throws IOException {
        URL resource = RoomMedia.class.getResource("/room1/" + name);
        if (resource != null) return resource;
        Path file = Path.of("resources", "room1", name);
        if (Files.isRegularFile(file)) return file.toUri().toURL();
        throw new IOException("resources/room1/" + name);
    }

    static BufferedImage picture(String name) {
        try {
            return ImageIO.read(resource(name + ".jpg"));
        } catch (IOException ex) {
            throw new IllegalStateException("1번 방 그림을 열 수 없습니다: " + name, ex);
        }
    }

    /** 패널이 보이는 동안만 소리 장치를 사용한다. 장치 열기는 Swing 이벤트 밖에서 수행한다. */
    static final class Audio {
        private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "room1-audio");
            thread.setDaemon(true);
            return thread;
        });
        private final Map<String, Clip> clips = new HashMap<>();
        private volatile boolean closed;
        private boolean silent = GraphicsEnvironment.isHeadless();

        private Clip clip(String name) {
            if (silent || closed) return null;
            Clip cached = clips.get(name);
            if (cached != null) return cached;
            Clip opened = null;
            try (AudioInputStream input = AudioSystem.getAudioInputStream(resource(name + ".wav"))) {
                opened = AudioSystem.getClip();
                opened.open(input);
                clips.put(name, opened);
                return opened;
            } catch (Exception ex) {
                if (opened != null) opened.close();
                silent = true;
                return null;
            }
        }

        void play(String name) {
            if (closed) return;
            worker.execute(() -> {
                Clip effect = clip(name);
                if (effect == null || closed) return;
                effect.stop();
                effect.setFramePosition(0);
                effect.start();
            });
        }

        void close() {
            closed = true;
            worker.execute(() -> clips.values().forEach(Clip::close));
            worker.shutdown();
        }
    }
}
