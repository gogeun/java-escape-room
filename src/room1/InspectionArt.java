package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;
import room1.RoomProgress.Station;

/** Layered inspection illustrations. All grips and moving parts use the same 680 × 340 space. */
final class InspectionArt {
    static final int CHAIN_TRAVEL = 97;
    static final int DRAWER_TRAVEL = 124;
    static final Rectangle HANDLE = new Rectangle(156, 110, 138, 89);
    static final Rectangle CHART = new Rectangle(367, 105, 155, 97);
    static final Rectangle TAG = new Rectangle(363, 119, 189, 110);
    static final Rectangle TOKEN = new Rectangle(456, 226, 56, 29);
    private static final int WIDTH = 680, HEIGHT = 340;
    private static final Color INK = new Color(63, 48, 34);
    private static final Color LIGHT = new Color(255, 219, 148);
    private final BufferedImage background;
    private BufferedImage openDrawer, wheel, lid, document;

    InspectionArt(Station station) {
        switch (station) {
            case DESK -> {
                background = RoomMedia.picture("closed-drawer.jpg");
                openDrawer = RoomMedia.picture("open-drawer.jpg");
                wheel = RoomMedia.picture("valve-handwheel.png");
                document = drawDocument(false);
            }
            case LAMP -> {
                background = RoomMedia.picture("inspection-lamp.jpg");
                document = drawDocument(true);
            }
            case TUBE -> {
                background = RoomMedia.picture("speaking-tube.jpg");
                lid = RoomMedia.picture("tube-cap.png");
            }
            default -> throw new IllegalArgumentException("Unsupported inspection station " + station);
        }
    }

    static Rectangle drawerGrip(double progress) {
        return new Rectangle(261, 110 + (int)Math.round(DRAWER_TRAVEL * progress), 158, 64);
    }

    static Rectangle chainGrip(double extension) {
        return new Rectangle(210, 168 + (int)Math.round(CHAIN_TRAVEL * extension), 35, 47);
    }

    static Rectangle lidGrip(double opening) {
        int left = 96 + (int)Math.min(0, 242*Math.cos(opening*1.98));
        return new Rectangle(left, 8, 325-left, 231);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static void glow(Graphics2D g, double x, double y, double rx, double ry, Color color) {
        Graphics2D layer = (Graphics2D)g.create();
        layer.translate(x, y);
        layer.scale(rx, ry);
        layer.setPaint(new RadialGradientPaint(0, 0, 1, new float[]{0, .52f, 1},
                new Color[]{color, new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()/3),
                        new Color(color.getRed(), color.getGreen(), color.getBlue(), 0)}));
        layer.fill(new Ellipse2D.Double(-1, -1, 2, 2));
        layer.dispose();
    }

    void drawer(Graphics2D g, double progress, boolean hasWheel, boolean hasChart) {
        g.drawImage(background, 0, 0, WIDTH, HEIGHT, null);
        if (progress <= 0) return;
        int frontY = 86 + (int)Math.round(DRAWER_TRAVEL * progress);
        int side = 78 - (int)Math.round(19 * progress);
        int bottom = frontY + (int)Math.round(88 + 8 * progress);
        Graphics2D draw = (Graphics2D)g.create();
        if (progress < .09) draw.setComposite(AlphaComposite.SrcOver.derive((float)(progress/.09)));
        glow(draw, 340, bottom, 312, 13 + 6 * progress, new Color(16, 11, 6, 150));

        // The box slides out from beneath the desk lip. The tray and its contents travel together.
        Path2D interior = new Path2D.Double();
        interior.moveTo(78, 84);
        interior.lineTo(602, 84);
        interior.lineTo(WIDTH-side, frontY+3);
        interior.lineTo(side, frontY+3);
        interior.closePath();
        Graphics2D tray = (Graphics2D)draw.create();
        tray.clip(interior);
        tray.drawImage(openDrawer, side, 83, WIDTH-side, frontY+4,
                side*2, 166, (WIDTH-side)*2, 424, null);
        int offset = frontY-210;
        tray.translate(0, offset);
        if (hasWheel) {
            glow(tray, 229, 168, 65, 26, new Color(15, 17, 12, 185));
            tray.drawImage(wheel, 146, 106, 158, 90, null);
        }
        if (hasChart) {
            tray.rotate(.045, 443, 154);
            tray.drawImage(document, 368, 106, 151, 96, null);
        }
        tray.dispose();

        // Use the real photographed fascia throughout the travel, growing as it comes forward.
        draw.drawImage(openDrawer, side, frontY, WIDTH-side, bottom, 118, 420, 1242, 614, null);
        draw.dispose();
    }

    void lamp(Graphics2D g, double light, double extension, boolean steaming, boolean hasTag) {
        g.drawImage(background, 0, 0, WIDTH, HEIGHT, null);
        if (light>0) {
            glow(g, 265, 226, 269, 192, new Color(255, 172, 48, (int)(light*125)));
            glow(g, 156, 169, 127, 59, new Color(255, 204, 99, (int)(light*108)));
            Graphics2D bulb = (Graphics2D)g.create();
            bulb.clip(new Ellipse2D.Double(140, 129, 53, 42));
            glow(bulb, 164, 145, 45, 44, new Color(255, 247, 192, (int)(light*248)));
            bulb.dispose();
            glow(g, 163, 153, 32, 28, new Color(255, 224, 139, (int)(light*180)));
        }
        drawChain(g, extension);
        if (hasTag) {
            Graphics2D tag = (Graphics2D)g.create();
            tag.rotate(-.026 + .024 * extension, 461, 91);
            tag.setColor(new Color(41, 34, 23));
            tag.setStroke(new BasicStroke(2.5f));
            tag.drawLine(461, 90, 398, 127);
            tag.drawLine(461, 90, 521, 127);
            tag.setColor(new Color(175, 139, 84));
            tag.setStroke(new BasicStroke(.9f));
            tag.drawLine(461, 89, 398, 126);
            tag.drawLine(461, 89, 521, 126);
            tag.drawImage(document, 362, 119, 191, 109, null);
            tag.setColor(new Color(20, 29, 23, (int)(135 * (1-light))));
            tag.fillRoundRect(366, 121, 182, 102, 7, 7);
            tag.dispose();
        }
        if (steaming) {
            for (int i=0; i<8; i++) {
                glow(g, 210+i*24, 224-i*12 + 6*Math.sin(i*2.1), 65-i*2, 43,
                        new Color(218, 213, 189, 87-i*6));
            }
        }
    }

    private static void drawChain(Graphics2D g, double extension) {
        double bottom = 186 + CHAIN_TRAVEL*extension;
        double sway = -3.8*Math.sin(extension*Math.PI);
        Graphics2D chain = (Graphics2D)g.create();
        double length = bottom-115;
        for (double d=0; d<length; d+=4.5) {
            double x = 227+sway*(d/length);
            double y = bottom-d;
            boolean edge = ((int)Math.round(d/4.5)&1)==0;
            double w = edge ? 4.2 : 2.2;
            Shape link = new Ellipse2D.Double(x-w/2, y-3.6, w, 7);
            chain.setStroke(new BasicStroke(2.6f));
            chain.setColor(new Color(57, 37, 20));
            chain.draw(link);
            chain.setPaint(new GradientPaint((float)x-2, (float)y-2, LIGHT, (float)x+2, (float)y+1, new Color(105, 72, 38)));
            chain.setStroke(new BasicStroke(1.4f));
            chain.draw(link);
        }
        double x = 227+sway;
        Shape ring = new Ellipse2D.Double(x-9, bottom+2, 18, 23);
        chain.setColor(new Color(38, 25, 15));
        chain.setStroke(new BasicStroke(5));
        chain.draw(ring);
        chain.setPaint(new GradientPaint((float)x-8, (float)bottom+4, LIGHT,
                (float)x+8, (float)bottom+22, new Color(151, 97, 45)));
        chain.setStroke(new BasicStroke(3.3f));
        chain.draw(ring);
        chain.dispose();
    }

    void tube(Graphics2D g, double opening, double age, boolean tokenPresent) {
        g.drawImage(background, 0, 0, WIDTH, HEIGHT, null);
        // Inside the glass transport tube, the brass disc passes behind both end collars.
        if (tokenPresent && opening>0 && age<.63) {
            Graphics2D glass = (Graphics2D)g.create();
            glass.clipRect(465, 60, 31, 44);
            token(glass, 481, 29 + 143 * age, 7, 2.3, age*4);
            glass.dispose();
        }
        if (opening>.3 && age<1.26) {
            double pulse = Math.sin(Math.PI*clamp((age-.15)/.9));
            glow(g, 169, 144, 35, 48, new Color(199, 123, 41, (int)(77*pulse*pulse)));
        }
        // A projecting hinge gives the cover thickness and reveals the inner lip at the edge.
        double angle = opening*1.98;
        double span = 242*Math.cos(angle);
        Graphics2D cover = (Graphics2D)g.create();
        cover.translate(100, 125);
        cover.shear(0, -.042*Math.sin(angle));
        cover.scale(span/242, 1+.044*Math.sin(angle));
        cover.drawImage(lid, -5, -121, 242, 243, null);
        cover.dispose();
        if (opening>0 && Math.abs(span)<15) {
            g.setColor(new Color(214, 165, 80, (int)(165*(1-Math.abs(span)/15))));
            g.setStroke(new BasicStroke(2.2f));
            g.drawLine(100, 19, 100, 232);
        }
        if (opening>.45 && tokenPresent && age>.63) {
            double fall = clamp((age-.63)/.35);
            double bounceAge = Math.max(0, age-.98);
            double bounce = 7*Math.abs(Math.sin(18*bounceAge))*Math.exp(-7*bounceAge);
            double y = 193 + 54*fall*fall - bounce;
            double x = 478+6*fall;
            glow(g, 484, 251, 17, 3, new Color(47, 26, 11, (int)(155*fall)));
            token(g, x, y, 14, 5.5+5*(1-fall)+bounce/4, -.2*(1-fall));
        }
    }

    private static void token(Graphics2D g, double x, double y, double rx, double ry, double angle) {
        Graphics2D coin = (Graphics2D)g.create();
        coin.translate(x, y);
        coin.rotate(angle);
        coin.scale(rx/14, ry/12);
        coin.setPaint(new GradientPaint(-10, -10, new Color(255, 220, 144), 12, 11, new Color(133, 78, 34)));
        coin.fillOval(-14, -12, 28, 24);
        coin.setColor(new Color(111, 67, 29));
        coin.setStroke(new BasicStroke(1));
        coin.drawOval(-11, -9, 22, 18);
        Path2D wing = new Path2D.Double();
        wing.moveTo(-8, -3); wing.lineTo(0, 1); wing.lineTo(8, -4); wing.lineTo(0, 6); wing.closePath();
        coin.fill(wing);
        coin.dispose();
    }

    private static BufferedImage drawDocument(boolean metal) {
        int w=360, h=202;
        BufferedImage image = new BufferedImage(w+12, h+12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        Brass.smooth(g);
        g.translate(4, 3);
        g.setColor(new Color(18, 16, 12, 150));
        g.fillRoundRect(3, 5, w, h, 9, 9);
        g.setPaint(new GradientPaint(2, 0, metal ? new Color(223, 181, 111) : new Color(251, 232, 183),
                w-10, h, metal ? new Color(136, 97, 56) : new Color(199, 170, 125)));
        g.fillRoundRect(0, 0, w, h, 9, 9);
        Random random = new Random(metal ? 20 : 16);
        for (int i=0; i<2200; i++) {
            int x=5+random.nextInt(w-10), y=5+random.nextInt(h-10);
            g.setColor(random.nextBoolean() ? new Color(63, 47, 28, 33) : new Color(255, 240, 199, 47));
            g.drawLine(x, y, Math.min(w-3, x+(metal ? random.nextInt(9) : 1)), y);
        }
        g.setColor(INK);
        g.drawRoundRect(5, 5, w-11, h-11, 6, 6);
        g.drawRoundRect(10, 10, w-21, h-21, 3, 3);
        if (metal) {
            g.fillOval(66, 11, 6, 6);
            g.fillOval(298, 11, 6, 6);
            text(g, "기관부 정비 표찰", w/2, 54, 24);
            g.drawLine(43, 71, w-43, 71);
            text(g, "연료 차단   →   냉각", w/2, 116, 25);
            text(g, "증기 배출   →   시간축 잠금", w/2, 157, 22);
        } else {
            text(g, "기관 압력 정비표", w/2, 54, 22);
            g.drawLine(35, 66, w-35, 66);
            g.setStroke(new BasicStroke(4));
            g.drawArc(47, 106, 63, 55, 10, 160);
            g.drawLine(78, 134, 96, 111);
            text(g, "안전 압력   50", 226, 134, 27);
            g.setStroke(new BasicStroke(1));
            g.drawLine(41, 165, w-41, 165);
        }
        g.dispose();
        return image;
    }

    private static void text(Graphics2D g, String value, int x, int y, int size) {
        g.setColor(INK);
        g.setFont(Brass.font(Font.BOLD, size));
        g.drawString(value, x-g.getFontMetrics().stringWidth(value)/2, y);
    }
}
