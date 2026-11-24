package room1;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/** The book illustration and the playable scene share one image and one camera frame. */
public final class WorkshopView {
    private static final BufferedImage BACKDROP = buildBackdrop();

    private WorkshopView() {}

    public static BufferedImage backdrop() { return BACKDROP; }

    private static BufferedImage buildBackdrop() {
        BufferedImage room = RoomMedia.picture("workshop.jpg");
        BufferedImage regulator = RoomMedia.picture("pressure-regulator.jpg");
        // The painting includes a red wheel. Seat the puzzle's bare casting on that spindle
        // before the book or the room gets a frame; the removable wheel is painted separately.
        BufferedImage casting = regulator.getSubimage(98, 3, 570, 570);
        BufferedImage mount = new BufferedImage(120, 122, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D tile = mount.createGraphics();
        Image reduced = casting.getScaledInstance(mount.getWidth(), mount.getHeight(), Image.SCALE_AREA_AVERAGING);
        tile.drawImage(new ImageIcon(reduced).getImage(), 0, 0, null);
        tile.scale(mount.getWidth(), mount.getHeight());
        tile.setComposite(AlphaComposite.DstIn);
        tile.setPaint(new RadialGradientPaint(.5f, .5f, .5f, new float[] {0, .92f, 1},
                new Color[] {Color.WHITE, Color.WHITE, new Color(255, 255, 255, 0)}));
        tile.fillRect(0, 0, 1, 1);
        tile.dispose();

        BufferedImage backdrop = new BufferedImage(room.getWidth(), room.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D scene = backdrop.createGraphics();
        scene.drawImage(room, 0, 0, null);
        scene.drawImage(mount, 498, 321, null);
        scene.dispose();
        return backdrop;
    }

    public static Rectangle bounds(int width, int height) {
        double cover = Math.max(width / 1280d, height / 720d);
        // On unusually tall or wide monitors keep all seven stations within reach.
        double scale = Math.min(cover, Math.min(width / 1130d, height / 610d));
        int w = (int) Math.round(1280 * scale), h = (int) Math.round(720 * scale);
        return new Rectangle((width - w) / 2, (height - h) / 2, w, h);
    }

    public static void preload() {
        backdrop();
        RoomMedia.picture("arrival.jpg");
        RoomMedia.picture("magnifier.png");
        RoomMedia.picture("magnifier-hover.png");
        RoomMedia.picture("tube-mount.png");
        RoomMedia.picture("shutdown-face.jpg");
        for (String file : new String[]{"closed-drawer.jpg", "open-drawer.jpg", "valve-handwheel.png",
                "inspection-lamp.jpg", "speaking-tube.jpg", "tube-cap.png",
                "pressure-regulator.jpg", "shutdown-instrument.png"}) {
            RoomMedia.picture(file);
        }
        LiftJourney.preload();
    }
}
