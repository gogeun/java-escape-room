package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.function.IntToDoubleFunction;
import room1.RoomProgress.Command;

/** The same four fittings as the workshop's control bank, separated into a bed and moving parts. */
final class ShutdownInstrument {
    private static final Color CREAM = new Color(250, 235, 200);
    private static final Color BRASS = new Color(219, 179, 114);
    private static final Color MINT = new Color(125, 223, 168);
    private static final Color VERMILION = new Color(250, 114, 76);
    private static final Color DARK = new Color(34, 33, 24);
    private static final Color IDLE = new Color(88, 100, 79);
    private static final Color[] CHANNEL = {
        new Color(156, 211, 216), new Color(229, 193, 128),
        new Color(226, 143, 108), new Color(151, 198, 177)
    };
    private static final Command[] FITTINGS = {Command.COOL, Command.AXIS, Command.FUEL, Command.VENT};
    static final int COUNT = FITTINGS.length;
    private static final int[] X = {84, 218, 352, 486};
    private static final String[] DIRECTION = {"↑  위로 올리기", "→  오른쪽 밀기", "↓  아래로 내리기", "↓  아래로 당기기"};
    private static final int SOURCE_SCALE = 2;
    private final BufferedImage plate, ivory, bolt, leather, ring;
    private BufferedImage engraved;

    ShutdownInstrument() {
        BufferedImage atlas = RoomMedia.picture("shutdown-instrument.png");
        plate = slice(atlas, 0, 0, 570, 232);
        ivory = slice(atlas, 10, 239, 32, 50);
        bolt = slice(atlas, 70, 240, 34, 40);
        leather = slice(atlas, 124, 239, 38, 51);
        ring = slice(atlas, 186, 245, 43, 42);
    }

    private static BufferedImage slice(BufferedImage atlas, int x, int y, int w, int h) {
        return atlas.getSubimage(x * SOURCE_SCALE, y * SOURCE_SCALE, w * SOURCE_SCALE, h * SOURCE_SCALE);
    }

    static Command control(int index) { return FITTINGS[index]; }

    /** Drawing and hit testing use the same moving grip, including during spring return. */
    static Point2D grip(int index, double t) {
        int x = X[index];
        return switch (control(index)) {
            case COOL -> new Point2D.Double(x, 143 - 64*t);
            case AXIS -> new Point2D.Double(x - 29 + 58*t, 111);
            case FUEL -> new Point2D.Double(x, 79 + 64*t);
            case VENT -> new Point2D.Double(x, 96 + 64*t);
        };
    }

    void paint(Graphics2D g, RoomProgress state, IntToDoubleFunction position,
               int focused, int dragging, int animating, boolean rejected, double age) {
        drawEngravedBed(g);
        boolean tripped = animating >= 0 && rejected && age < .65;
        drawRegister(g, state.step(), tripped, animating >= 0, age);
        for (int i = 0; i < FITTINGS.length; i++) {
            Point2D handle = grip(i, position.applyAsDouble(i));
            int x = X[i];
            Command control = FITTINGS[i];
            boolean engaged = state.engaged(control);
            boolean focus = i == focused || i == dragging;
            boolean fault = i == animating && tripped;
            Color contact = fault ? VERMILION : engaged ? MINT : CHANNEL[i];
            if (focus) drawFocus(g, x, contact);
            jewel(g, x + 49, 58, 4, fault || engaged || i == dragging ? contact : IDLE, fault || engaged);
            if (control == Command.AXIS) {
                drawBolt(g, (int)Math.round(handle.getX()));
            } else if (control == Command.VENT) {
                drawRing(g, x, (int)Math.round(handle.getY()));
                if (engaged && i == animating && age < .65) drawSteam(g, x, age);
            } else {
                drawLever(g, x, handle.getY(), control == Command.COOL);
            }
            centered(g, fault ? "안전 차단" : engaged ? "접점 고정" : DIRECTION[i],
                    x, 214, 11, contact, false);
        }
    }

    /** Bake the enamel, stationary graduations and label lettering at the actual display size. */
    private void drawEngravedBed(Graphics2D g) {
        AffineTransform view = g.getTransform();
        int w = Math.max(1, (int)Math.round(570 * view.getScaleX()));
        int h = Math.max(1, (int)Math.round(232 * view.getScaleY()));
        if (engraved == null || engraved.getWidth() != w || engraved.getHeight() != h) {
            engraved = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D base = engraved.createGraphics();
            Brass.smooth(base);
            base.scale(w / 570d, h / 232d);
            base.drawImage(plate, 0, 0, 570, 232, null);
            engrave(base);
            base.dispose();
        }
        Graphics2D nativePixels = (Graphics2D)g.create();
        nativePixels.setTransform(new AffineTransform());
        nativePixels.drawImage(engraved, (int)Math.round(view.getTranslateX()),
                (int)Math.round(view.getTranslateY()), null);
        nativePixels.dispose();
    }

    private static void engrave(Graphics2D g) {
        g.setFont(Brass.font(Font.BOLD, 14));
        g.setColor(CREAM);
        g.drawString("정지 회로", 35, 30);
        // The four lamps follow the sequence, independent of the layout of the fittings.
        wire(g, 170, 26, 385, 26, 4, new Color(22, 30, 25));
        wire(g, 170, 25, 385, 25, 1.4f, new Color(128, 108, 72));
        for (int i=0; i<FITTINGS.length; i++) {
            int x=X[i];
            centered(g, FITTINGS[i].label, x, 200, 14, CREAM, true);
            if (FITTINGS[i] == Command.COOL || FITTINGS[i] == Command.FUEL) {
                for (int j=0; j<7; j++) {
                    int y=79 + 10*j;
                    wire(g,x+22,y,x+(j%3 == 0 ? 28 : 25),y,1,new Color(219,189,137,174));
                }
            }
            if (FITTINGS[i] == Command.AXIS) {
                for (int j=0; j<7; j++) {
                    int tick=x-30+j*10;
                    wire(g,tick,142,tick,j%3 == 0 ? 147 : 145,1,new Color(201,172,119,150));
                }
            }
        }
    }

    private static void drawRegister(Graphics2D g, int steps, boolean tripped, boolean justMoved, double age) {
        for (int i = 0; i < 4; i++) {
            int x = 218 + 39 * i;
            boolean closed = i < steps;
            jewel(g, x, 25, 7, tripped ? VERMILION : closed ? MINT : IDLE, tripped || closed);
            if (closed && !tripped && i == steps-1 && justMoved && age < .55) {
                double t = age / .55;
                g.setColor(new Color(176, 243, 183, (int)(160 * (1-t))));
                double r = 7 + t*6;
                g.setStroke(new BasicStroke(1.2f));
                g.draw(new Ellipse2D.Double(x-r, 25-r, 2*r, 2*r));
            }
        }
        String status = tripped ? "안전 차단" : steps == 4 ? "기관 정지" : steps == 0 ? "회로 대기" : "정지 진행";
        g.setFont(Brass.font(Font.BOLD, 12));
        g.setColor(tripped ? VERMILION : steps > 0 ? MINT : BRASS);
        g.drawString(status, 423, 30);
        centered(g, steps + " / 4", 514, 30, 14, CREAM, true);
    }

    private void drawLever(Graphics2D g, int x, double y, boolean cooling) {
        int height = (int)Math.round(24 + 8*Math.abs(y-111)/32);
        // The stem foreshortens through its centre; the grip stays under the player's hand.
        wire(g, x+2, 115, x+3, y+3, 10, new Color(18, 17, 13, 176));
        wire(g, x, 112, x, y, 7, new Color(107, 71, 37));
        wire(g, x-1, 112, x-1, y, 3, BRASS);
        g.setPaint(new GradientPaint(x-7, 104, new Color(238, 199, 126), x+6, 119, new Color(94, 63, 36)));
        g.fillOval(x-7, 104, 14, 15);
        g.setColor(DARK);
        g.drawOval(x-7, 104, 14, 15);
        int width = cooling ? 24 : 29;
        g.setColor(new Color(16, 17, 12, 106));
        g.fillOval(x-width/2+3, (int)y-height/2+5, width+1, height);
        g.drawImage(cooling ? ivory : leather, x-width/2, (int)Math.round(y-height/2.0), width, height, null);
    }

    private void drawBolt(Graphics2D g, int gripX) {
        g.setColor(new Color(21, 23, 17, 160));
        g.fillRoundRect(gripX-12, 99, 31, 31, 6, 6);
        g.drawImage(bolt, gripX-15, 94, 30, 35, null);
    }

    private void drawRing(Graphics2D g, int x, int gripY) {
        for (int y = 69, n = 0; y < gripY-12; y += 6, n++) {
            int half = n % 2 == 0 ? 3 : 1;
            g.setColor(new Color(65, 43, 24));
            g.setStroke(new BasicStroke(3));
            g.drawOval(x-half, y, half*2, 8);
            g.setColor(BRASS);
            g.setStroke(new BasicStroke(1.4f));
            g.drawArc(x-half, y, half*2, 8, 52, 179);
        }
        g.setColor(new Color(12, 22, 18, 115));
        g.setStroke(new BasicStroke(4));
        g.drawOval(x-10, gripY-10, 24, 26);
        g.drawImage(ring, x-15, gripY-15, 30, 30, null);
    }

    private static void drawSteam(Graphics2D original, int x, double seconds) {
        Graphics2D g = (Graphics2D)original.create();
        double progress = Math.min(1, seconds / .65);
        g.clipRect(x+14, 45, 41, 92);
        for (int n = 0; n < 5; n++) {
            double t = progress - n*.08;
            if (t < 0 || t > 1) continue;
            int px = (int)(x + 19 + 20*t), py = (int)(87 - 33*t + 3*Math.sin(t*5+n));
            int radius = 3 + (int)(12*t);
            Color smoke = new Color(229, 234, 214, (int)(125 * (1-progress)));
            g.setPaint(new RadialGradientPaint(px, py, radius, new float[]{0, 1},
                    new Color[]{smoke, new Color(215, 229, 213, 0)}));
            g.fillOval(px-radius, py-radius, 2*radius, 2*radius);
        }
        g.dispose();
    }

    private static void drawFocus(Graphics2D g, int x, Color accent) {
        g.setStroke(new BasicStroke(1.3f));
        g.setColor(accent);
        for (int side : new int[]{-1, 1}) {
            int edge = x + side*60;
            g.drawLine(edge, 52, edge, 60);
            g.drawLine(edge, 52, edge-side*9, 52);
            g.drawLine(edge, 161, edge, 169);
            g.drawLine(edge, 169, edge-side*9, 169);
        }
    }

    private static void jewel(Graphics2D g, int x, int y, int radius, Color color, boolean lit) {
        int r = radius;
        if (lit) {
            g.setPaint(new RadialGradientPaint(x, y, 2.3f*r, new float[]{0, 1},
                    new Color[]{new Color(color.getRed(), color.getGreen(), color.getBlue(), 121),
                            new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
            g.fillOval(x-3*r, y-3*r, 6*r, 6*r);
        }
        g.setPaint(new GradientPaint(x-r, y-r, BRASS, x+r, y+r, DARK));
        g.fillOval(x-r-1, y-r-1, 2*r+2, 2*r+2);
        g.setPaint(new RadialGradientPaint(x-.3f*r, y-.4f*r, 1.65f*r, new float[]{0,.45f,1},
                new Color[]{color.brighter(), color, color.darker().darker()}));
        g.fillOval(x-r+1, y-r+1, 2*r-2, 2*r-2);
        g.setColor(new Color(255, 250, 211, lit ? 188 : 81));
        g.drawLine(x-r/2, y-r/2, x, y-r/2);
    }

    private static void centered(Graphics2D g, String text, int x, int baseline, int size, Color color, boolean bold) {
        g.setFont(Brass.font(bold ? Font.BOLD : Font.PLAIN, size));
        int left = x - g.getFontMetrics().stringWidth(text)/2;
        g.setColor(new Color(12, 19, 17));
        g.drawString(text, left, baseline+1);
        g.setColor(color);
        g.drawString(text, left, baseline);
    }

    private static void wire(Graphics2D g, double x1, double y1, double x2, double y2, float width, Color color) {
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(color);
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }
}
