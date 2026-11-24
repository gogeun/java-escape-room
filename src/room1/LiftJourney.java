package room1;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;

/** One continuous shot from the painted workshop gate to the hall on the lower landing. */
final class LiftJourney {
    static final double DURATION = 5.6;
    static final double DRIVE_START = 2.45;
    private static final double DOCK = 4.78;
    private static final int W = 1280, H = 720;
    private static final double GATE_X = 1131.5, GATE_Y = 316;
    private static final Path2D OPENING = gateOpening();
    private static final int SHAFT_X = 178, SHAFT_WIDTH = W - 2 * SHAFT_X;
    private static final int SHAFT_TOP = 58, SHAFT_HEIGHT = SHAFT_WIDTH * 3 / 2;
    private static final int LEFT_TRIM = 158, RIGHT_TRIM = 1049, TRIM_TOP = 91;

    private LiftJourney() {}

    private static Path2D gateOpening() {
        Path2D path = new Path2D.Double();
        path.moveTo(1051, 499);
        path.lineTo(1051, 161);
        path.quadTo(1054, 137, 1082, 134);
        path.quadTo(1133, 126, 1180, 134);
        path.quadTo(1204, 136, 1211, 160);
        path.lineTo(1211, 504);
        path.closePath();
        return path;
    }

    private static final class Art {
        static final BufferedImage SHAFT = prepare("lift-shaft.jpg", SHAFT_WIDTH, SHAFT_HEIGHT, BufferedImage.TYPE_INT_RGB);
        static final BufferedImage HALL = prepare("arrival.jpg", W, H, BufferedImage.TYPE_INT_RGB);
        static final BufferedImage CABIN = prepare("lift-cabin.png", W, H, BufferedImage.TYPE_INT_ARGB_PRE);
        static final BufferedImage LEFT_REFLECTION = reflection(LEFT_TRIM);
        static final BufferedImage RIGHT_REFLECTION = reflection(RIGHT_TRIM);
        static final BufferedImage BOARDING = boarding();

        private static BufferedImage prepare(String file, int w, int h, int format) {
            BufferedImage image = new BufferedImage(w, h, format);
            Graphics2D g = image.createGraphics();
            Brass.smooth(g);
            g.drawImage(RoomMedia.picture(file), 0, 0, w, h, null);
            g.dispose();
            return image;
        }

        /** Only the photographed polished metal picks up blue light from the lower landing. */
        private static BufferedImage reflection(int left) {
            int w = 73, h = 568;
            BufferedImage strip = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            for (int y=0; y<h; y++) {
                double vertical = Math.sin(Math.PI * (y+.5)/h);
                for (int x=0; x<w; x++) {
                    int pixel = CABIN.getRGB(left+x, TRIM_TOP+y);
                    int r = (pixel >>> 16) & 255, g = (pixel >>> 8) & 255, b = pixel & 255;
                    double metal = (r+g+b)/765d;
                    int alpha = (int)((pixel >>> 24) * metal * vertical * Math.sin(Math.PI*(x+.5)/w));
                    int red = (int)(.80*r+14), green = (int)(.84*g+35), blue = Math.min(255, (int)(.91*b+72));
                    strip.setRGB(x, y, alpha<<24 | red<<16 | green<<8 | blue);
                }
            }
            return strip;
        }

        private static BufferedImage boarding() {
            BufferedImage image = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            Brass.smooth(g);
            drawShaft(g, 0);
            g.drawImage(CABIN, 0, 0, null);
            g.dispose();
            return image;
        }
    }

    static void preload() { Art.BOARDING.getWidth(); }

    /** The gameplay frame fits every hotspot; as the camera boards it fills even tall windows. */
    static Rectangle frame(Rectangle resting, int width, int height, double fraction) {
        double t = ease(fraction * DURATION / 1.1);
        double cover = Math.max(width / (double)W, height / (double)H);
        int w = (int)Math.round(resting.width + (W * cover - resting.width) * t);
        int h = (int)Math.round(resting.height + (H * cover - resting.height) * t);
        return new Rectangle((width-w)/2, (height-h)/2, w, h);
    }

    static void paint(Graphics2D g, BufferedImage workshop, double fraction) {
        double seconds = fraction * DURATION;
        if (seconds < DRIVE_START) {
            board(g, workshop, seconds);
            return;
        }
        if (fraction >= 1) {
            g.drawImage(Art.HALL, 0, 0, W, H, null);
            return;
        }
        double descent = travel((seconds - DRIVE_START) / (DOCK - DRIVE_START));
        double exit = ease((seconds - 4.32) / (DURATION - 4.32));

        // The far wall moves less than the shaft lintels. At the lower landing the near
        // lintel uncovers the actual next room, which stays in view as we step over the sill.
        if (descent > .5) {
            double hallScale = .88 + .12 * ease((seconds - 4.0) / (DOCK - 4.0));
            int hallW = (int)Math.round(W * hallScale), hallH = (int)Math.round(H * hallScale);
            int hallY = (int)Math.round((H-hallH)*.5 + (1-descent)*235);
            g.drawImage(Art.HALL, (W-hallW)/2, hallY, hallW, hallH, null);
        }
        if (descent < 1) drawShaft(g, descent);

        double cabinScale = 1 + .98 * exit;
        if (cabinScale >= 1.91) return; // All four edges of the physical opening are already offscreen.
        double cabinW = W*cabinScale, cabinH = H*cabinScale;
        Graphics2D jambs = (Graphics2D)g.create();
        jambs.translate((W-cabinW)/2, (H-cabinH)/2 - 9*Math.sin(Math.PI*exit));
        jambs.scale(cabinScale, cabinScale);
        jambs.drawImage(Art.CABIN, 0, 0, null);
        double moon = ease((descent-.40)/.24) * (1-.66*ease((descent-.80)/.20));
        if (moon > 0) {
            jambs.setComposite(AlphaComposite.SrcOver.derive((float)moon));
            jambs.drawImage(Art.LEFT_REFLECTION, LEFT_TRIM, TRIM_TOP, null);
            jambs.drawImage(Art.RIGHT_REFLECTION, RIGHT_TRIM, TRIM_TOP, null);
        }
        jambs.dispose();
    }

    private static void drawShaft(Graphics2D g, double descent) {
        int top = (int)Math.round(SHAFT_TOP - descent * (SHAFT_TOP + SHAFT_HEIGHT - 50));
        g.drawImage(Art.SHAFT, SHAFT_X, top, null);
    }

    private static void board(Graphics2D g, BufferedImage workshop, double seconds) {
        double approach = ease((seconds-.04) / (DRIVE_START-.12));
        if (approach >= 1) {
            g.drawImage(Art.BOARDING, 0, 0, null);
            return;
        }
        double zoom = Math.pow(9.3, approach);
        // Track the gate toward the screen centre without looking past the edge of the painting.
        double x = W - (W-GATE_X) * Math.pow((W*.5)/(W-GATE_X), approach);
        double y = GATE_Y + (H*.5-GATE_Y)*approach;
        AffineTransform camera = new AffineTransform();
        camera.translate(x, y);
        camera.scale(zoom, zoom);
        camera.translate(-GATE_X, -GATE_Y);
        g.drawImage(workshop, camera, null);

        double opened = ease((seconds-.06) / 1.24);
        if (opened <= 0) return;
        Graphics2D portal = (Graphics2D)g.create();
        portal.clip(camera.createTransformedShape(OPENING));
        double cabinH = Math.min(H, 416*zoom);
        double cabinY = Math.min(H-cabinH, Math.max(0, y-cabinH*.5));
        double cabinScale = cabinH/H;
        AffineTransform interior = new AffineTransform();
        interior.translate(x-W*cabinScale*.5, cabinY);
        interior.scale(cabinScale, cabinScale);
        portal.drawImage(Art.BOARDING, interior, null);
        if (opened < 1) {
            // Slide the original painted leaves under their jambs without stretching the lattice.
            portal.transform(camera);
            double shift = opened * 85;
            portal.translate(-shift, 0);
            portal.drawImage(workshop, 1046, 127, 1132, 506, 1046, 127, 1132, 506, null);
            portal.translate(shift*2, 0);
            portal.drawImage(workshop, 1132, 127, 1216, 506, 1132, 127, 1216, 506, null);
        }
        portal.dispose();
    }

    private static double ease(double t) {
        t = Math.max(0, Math.min(t, 1));
        return t*t*t*(10+t*(-15+6*t));
    }

    /** Cosine acceleration into steady cable speed, mirrored as the brake takes up. */
    private static double travel(double t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        double ramp = .23;
        if (t > 1-ramp) return 1-travel(1-t);
        if (t < ramp) return (t-ramp/Math.PI*Math.sin(Math.PI*t/ramp)) / (2*(1-ramp));
        return (t-ramp*.5)/(1-ramp);
    }
}
