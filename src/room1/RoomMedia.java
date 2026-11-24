package room1;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

final class RoomMedia {
    private static final Map<String, BufferedImage> PICTURES = new ConcurrentHashMap<>();
    private RoomMedia() {}

    private static URL resource(String name) throws IOException {
        URL resource = RoomMedia.class.getResource("/room1/" + name);
        if (resource != null) {
            if ("file".equals(resource.getProtocol())) {
                try {
                    Path output = Path.of(resource.toURI()).getParent().getParent();
                    // Eclipse may retain a previous build's media in bin/room1.
                    if (output.endsWith("bin")) {
                        Path original = output.resolveSibling("resources").resolve("room1").resolve(name);
                        if (Files.isRegularFile(original)) return original.toUri().toURL();
                    }
                } catch (URISyntaxException ignored) {
                    // Keep using the URL supplied by the class loader.
                }
            }
            return resource;
        }
        Path file = common.Resources.file("resources/room1/" + name).toPath();   // [통합] 실행 폴더와 상관없이 찾기
        if (Files.isRegularFile(file)) return file.toUri().toURL();
        throw new IOException("resources/room1/" + name);
    }

    static BufferedImage picture(String name) {
        return PICTURES.computeIfAbsent(name, RoomMedia::readPicture);
    }

    private static BufferedImage readPicture(String name) {
        try {
            BufferedImage image = ImageIO.read(resource(name));
            if (image == null) throw new IOException("지원하지 않는 이미지: " + name);
            return image;
        } catch (IOException ex) {
            throw new IllegalStateException("1번 방 그림을 열 수 없습니다: " + name, ex);
        }
    }

    /** The mixer playback head drives the camera. All line access stays off the Swing thread. */
    static final class Impact {
        private enum Mode { WAITING, PLAYING, SILENT, DONE }
        private static final long SECOND = 1_000_000_000L;
        private static final long DEVICE_TIMEOUT = 650_000_000L;
        private Mode mode;
        private long lastAdvance, silentStart;
        private double playhead;
        private volatile Track track;

        private Impact(boolean audible, long now) {
            mode = audible ? Mode.WAITING : Mode.SILENT;
            lastAdvance = silentStart = now;
        }

        static Impact start(boolean soundEnabled) {
            boolean audible = soundEnabled && !GraphicsEnvironment.isHeadless();
            Impact impact = new Impact(audible, System.nanoTime());
            Thread player = new Thread(() -> impact.loadAndPlay(audible), "room1-impact");
            player.setDaemon(true);
            player.start();
            return impact;
        }

        private void loadAndPlay(boolean audible) {
            try (AudioInputStream input = AudioSystem.getAudioInputStream(resource("rumble.wav"))) {
                AudioFormat format = input.getFormat();
                byte[] pcm = input.readAllBytes();
                track = new Track(format, pcm);
                if (!audible) return;
                try (Clip line = AudioSystem.getClip()) {
                    line.open(format, pcm, 0, pcm.length);
                    if (!acceptDevice(System.nanoTime())) return;
                    line.start();
                    while (true) {
                        double at = line.getLongFramePosition() / (double)line.getFrameLength();
                        if (!reportFrame(at, System.nanoTime())) break;
                        if (at > 0 && !line.isRunning()) break;
                        Thread.sleep(8);
                    }
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                // An unavailable output device still gets a single, visual-only impact.
            } finally {
                if (audible) silentFallback(System.nanoTime());
            }
        }

        private synchronized boolean acceptDevice(long now) {
            if (mode != Mode.WAITING || now - lastAdvance >= DEVICE_TIMEOUT) return false;
            mode = Mode.PLAYING;
            lastAdvance = now;
            return true;
        }

        private synchronized boolean reportFrame(double fraction, long now) {
            if (mode != Mode.PLAYING) return false;
            if (fraction > playhead) {
                playhead = Math.min(1, fraction);
                lastAdvance = now;
                if (playhead == 1) mode = Mode.DONE;
            } else if (now - lastAdvance >= DEVICE_TIMEOUT) {
                silentFallback(now);
            }
            return mode == Mode.PLAYING;
        }

        private synchronized void silentFallback(long now) {
            if (mode != Mode.WAITING && mode != Mode.PLAYING) return;
            mode = Mode.SILENT;
            silentStart = now - (long)(playhead * durationNanos());
        }

        private long durationNanos() {
            return track == null ? SECOND : track.durationNanos;
        }

        synchronized double position(long now) {
            if ((mode == Mode.WAITING || mode == Mode.PLAYING) && now - lastAdvance >= DEVICE_TIMEOUT)
                silentFallback(now);
            if (mode == Mode.SILENT) {
                playhead = Math.max(0, Math.min(1, (now - silentStart) / (double)durationNanos()));
                if (playhead >= 1) mode = Mode.DONE;
            }
            return mode == Mode.DONE ? 1 : playhead;
        }

        double strength(double phase) {
            if (phase <= 0 || phase >= 1) return 0;
            Track sample = track;
            if (sample == null) return Math.min(1, phase * 45) * (1 - phase);
            double p = phase * (sample.energy.length - 1);
            int i = (int)p;
            return sample.energy[i] + (sample.energy[i+1] - sample.energy[i]) * (p-i);
        }

        synchronized void cancel() { mode = Mode.DONE; }

        private static final class Track {
            final long durationNanos;
            final double[] energy;

            Track(AudioFormat format, byte[] pcm) {
                // The packaged impact is signed 16-bit little-endian mono PCM.
                int frames = pcm.length / format.getFrameSize();
                int window = Math.max(1, Math.round(format.getFrameRate() / 60));
                durationNanos = Math.round(SECOND * (frames / (double)format.getFrameRate()));
                energy = new double[(frames + window - 1) / window + 2];
                double peak = 1;
                for (int start = 0; start < frames; start += window) {
                    int end = Math.min(frames, start + window);
                    double sum = 0;
                    for (int j = start; j < end; j++) {
                        int k = j * format.getFrameSize();
                        int value = (short)((pcm[k] & 255) | (pcm[k+1] << 8));
                        sum += (double)value * value;
                    }
                    double rms = Math.sqrt(sum / (end - start));
                    energy[1 + start / window] = rms;
                    peak = Math.max(peak, rms);
                }
                for (int i=0; i<energy.length; i++) energy[i] /= peak;
            }
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

        void prepare(String name) {
            if (!closed) worker.execute(() -> clip(name));
        }

        void play(String name) {
            play(name, 0);
        }

        void play(String name, double secondsIn) {
            if (closed) return;
            worker.execute(() -> {
                Clip effect = clip(name);
                if (effect == null || closed) return;
                long offset = Math.round(secondsIn * 1_000_000);
                if (offset >= effect.getMicrosecondLength()) return;
                effect.stop();
                effect.setMicrosecondPosition(offset);
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
