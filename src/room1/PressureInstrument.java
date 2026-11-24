package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Fixed casting, moving wheel, and pressure needle share the mechanism panel's logical coordinates. */
final class PressureInstrument {
    private static final int WIDTH = 570, HEIGHT = 232;
    private static final int WHEEL_X = 153, WHEEL_Y = 115;
    private static final int DIAL_X = 457, DIAL_Y = 101;
    private static final Color INK = new Color(64, 47, 32);
    private static final Color IVORY = new Color(243, 228, 196);
    private static final Color GREEN = new Color(43, 105, 83);
    private static final Color COPPER = new Color(167, 66, 39);
    private static final Font DIAL_NUMBERS = new Font(Font.SERIF, Font.BOLD, 11);
    private static final Font READOUT = new Font(Font.MONOSPACED, Font.BOLD, 18);
    private static final BufferedImage CASTING = RoomMedia.picture("pressure-regulator.jpg");
    private static final BufferedImage WHEEL = mountedWheel();
    private static final BufferedImage MIST = steamPuff();
    private BufferedImage backing;
    private boolean printedWithChart, printedWithShadow;
    private String printedInstruction;
    private BufferedImage instructionPlate;

    void paint(Graphics2D g, RoomProgress state, boolean holding, boolean focused, double lockAgeSeconds) {
        Graphics2D face = (Graphics2D) g.create();
        Brass.smooth(face);
        face.clipRect(0, 0, WIDTH, HEIGHT);
        boolean stable = state.solved() > 0;
        boolean hasChart = state.has(RoomProgress.Item.CHART) || stable;
        boolean inBand = Math.abs(state.pressure() - 50) <= 1.5;
        boolean installed = state.installed(RoomProgress.Item.HANDLE);
        BufferedImage fixed = backing(face,hasChart,installed);
        // The backing is already sampled for this viewport. A second bilinear pass over
        // the full plate spends a frame filtering pixels that are less than a pixel apart.
        face.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        face.drawImage(fixed, 0, 0, WIDTH, HEIGHT, null);
        face.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (installed) {
            if (holding || (focused && !stable)) {
                face.setStroke(new BasicStroke(1.25f));
                face.setColor(holding ? new Color(243, 209, 134, 210) : new Color(244, 219, 156, 164));
                face.drawArc(WHEEL_X-83, WHEEL_Y-83, 166, 166, 22, 36);
                face.drawArc(WHEEL_X-83, WHEEL_Y-83, 166, 166, 202, 36);
            }
            drawMountedWheel(face, WHEEL_X, WHEEL_Y, holding ? 76 : 77, state.turns());
        }
        else if (state.has(RoomProgress.Item.HANDLE)) {
            face.setStroke(new BasicStroke(focused ? 2f : 1.25f));
            face.setColor(new Color(239, 205, 128));
            face.drawArc(WHEEL_X-25,WHEEL_Y-25,50,50,20,64);
            face.drawArc(WHEEL_X-25,WHEEL_Y-25,50,50,200,64);
        }
        Graphics2D dial = (Graphics2D)face.create();
        dial.translate(DIAL_X, DIAL_Y);
        drawNeedle(dial,state.pressure());
        dial.setFont(READOUT);
        dial.setColor(stable ? new Color(173, 231, 185) : IVORY);
        centered(dial, Long.toString(Math.round(state.pressure())), 0, 51);
        dial.dispose();
        if (stable && lockAgeSeconds >= 0 && lockAgeSeconds < .65) drawSteam(face, lockAgeSeconds / .65);
        String instruction;
        if (!installed) instruction = state.has(RoomProgress.Item.HANDLE) ? "빈 축을 눌러 손잡이를 끼우세요." : "결합 축에서 밸브 손잡이가 빠져 있다.";
        else if (stable) instruction = "안전 압력 고정";
        else if (hasChart && inBand) instruction = "안전 눈금에 도달했다. 손을 놓아 고정하세요.";
        else instruction = "손잡이를 잡고 원을 그려 돌리세요.";
        if (!instruction.equals(printedInstruction)) {
            instructionPlate = new BufferedImage(WIDTH*2,60,BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D plate = instructionPlate.createGraphics();
            Brass.smooth(plate);
            plate.scale(2,2);
            plate.translate(0,-198);
            drawInstruction(plate, instruction, stable, inBand && hasChart);
            plate.dispose();
            printedInstruction = instruction;
        }
        face.drawImage(instructionPlate, 0, 198, WIDTH, 30, null);
        face.dispose();
    }

    /** Printing the dial and the shadow onto the stationary casting keeps rotary dragging cheap. */
    private BufferedImage backing(Graphics2D g, boolean hasChart, boolean mounted) {
        int w = Math.max(1, Math.min(CASTING.getWidth(), (int)Math.round(WIDTH * g.getTransform().getScaleX())));
        if (backing != null && backing.getWidth() == w && printedWithChart == hasChart && printedWithShadow == mounted) return backing;
        int h = Math.max(1, (int)Math.round(w * HEIGHT / (double) WIDTH));
        backing = new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
        Graphics2D fixed = backing.createGraphics();
        Brass.smooth(fixed);
        fixed.drawImage(CASTING,0,0,w,h,null);
        fixed.scale(w/(double)WIDTH,h/(double)HEIGHT);
        printDial(fixed,hasChart);
        if (mounted) {
            fixed.setPaint(new RadialGradientPaint(WHEEL_X+2, WHEEL_Y+5, 87,
                    new float[]{0, .72f, 1}, new Color[]{new Color(22, 13, 6, 105),
                    new Color(26, 17, 8, 117), new Color(27, 18, 12, 0)}));
            fixed.fillOval(WHEEL_X-86, WHEEL_Y-82, 176, 176);
        }
        fixed.dispose();
        printedWithChart = hasChart;
        printedWithShadow = mounted;
        return backing;
    }

    /** Straighten the drawer sprite's oblique view around the actual square spindle. */
    private static BufferedImage mountedWheel() {
        BufferedImage wheel = new BufferedImage(312, 312, BufferedImage.TYPE_INT_ARGB_PRE);
        BufferedImage object = RoomMedia.picture("valve-handwheel.png");
        Graphics2D g = wheel.createGraphics();
        Brass.smooth(g);
        g.drawImage(object, 0, 0, 312, 156, 44, 0, 351, 115, null);
        g.drawImage(object, 0, 156, 312, 312, 44, 115, 351, 264, null);
        // The casting's upper-left bolt has a lit bevel and a dark lower lip.
        // Trace its silhouette so the original wheel remains visible around the head.
        g.translate(156 - 281, 156 - 136);
        g.setPaint(new TexturePaint(CASTING, new Rectangle(0, 0, CASTING.getWidth(), CASTING.getHeight())));
        g.fillPolygon(new int[]{263, 280, 283, 298, 300, 300, 297, 282, 279, 260, 260},
                new int[]{123, 115, 115, 124, 128, 144, 149, 157, 157, 147, 144}, 11);
        g.dispose();
        return wheel;
    }

    static void drawMountedWheel(Graphics2D g, double x, double y, double radius, double turns) {
        Graphics2D hardware = (Graphics2D) g.create();
        hardware.translate(x, y);
        hardware.scale(radius / 77, radius / 77);
        hardware.rotate(turns * Math.PI * 2);
        hardware.drawImage(WHEEL, -77, -77, 154, 154, null);
        // An enamel index mark makes slow turns and complete revolutions easy to see.
        hardware.setColor(new Color(46, 44, 35));
        hardware.fillRoundRect(-3, -74, 7, 10, 3, 3);
        hardware.setColor(new Color(199, 224, 203));
        hardware.fillRoundRect(-2, -73, 5, 7, 2, 2);
        hardware.dispose();
    }

    private static double angle(double pressure) { return Math.toRadians(145 + 2.5 * pressure); }

    private static void printDial(Graphics2D g, boolean hasChart) {
        Graphics2D dial = (Graphics2D)g.create();
        dial.translate(DIAL_X, DIAL_Y);
        if (hasChart) {
            dial.setColor(GREEN);
            dial.setStroke(new BasicStroke(3.4f));
            dial.draw(new Arc2D.Double(-71, -71, 142, 142, 86.25, 7.5, Arc2D.OPEN));
            dial.fillPolygon(new int[]{-2, 2, 0}, new int[]{-66, -66, -62}, 3);
        }
        dial.setFont(DIAL_NUMBERS);
        for (int value=0; value<=100; value+=2) {
            double a=angle(value), x=Math.cos(a), y=Math.sin(a);
            boolean major=value%10==0;
            int inner=major ? 56 : 61;
            dial.setColor(value>=80 ? COPPER : INK);
            dial.setStroke(new BasicStroke(major ? 1.6f : .8f));
            dial.draw(new Line2D.Double(x*inner,y*inner,x*66,y*66));
            if (value%20==0 || value==50) {
                if (value==50 && hasChart) dial.setColor(GREEN);
                centered(dial, Integer.toString(value), (int)Math.round(x*47), (int)Math.round(y*47)+4);
            }
        }
        dial.setFont(Brass.font(Font.BOLD, 11));
        dial.setColor(INK);
        centered(dial, "증기 압력", 0, -19);
        dial.setColor(new Color(126, 91, 58));
        dial.fillRoundRect(-30, 34, 60, 22, 5, 5);
        dial.setPaint(new GradientPaint(0, 34, new Color(28, 34, 29), 0, 56, new Color(58, 60, 47)));
        dial.fillRoundRect(-29, 35, 58, 20, 4, 4);
        dial.setFont(new Font(Font.SERIF, Font.BOLD, 11));
        dial.setColor(INK);
        centered(dial, "kPa", 0, 68);
        dial.dispose();
    }

    private static void drawNeedle(Graphics2D dial, double pressure) {
        Graphics2D needle=(Graphics2D)dial.create();
        needle.rotate(angle(pressure));
        Polygon tip=new Polygon(new int[]{-17, -11, 56, 58, 2, -11}, new int[]{-1,-3,-1,0,2,3},6);
        needle.translate(1,2);
        needle.setColor(new Color(56, 39, 25, 92));
        needle.fill(tip);
        needle.translate(-1,-2);
        needle.setPaint(new GradientPaint(0,-3,new Color(178,72,37),0,3,new Color(72,35,27)));
        needle.fill(tip);
        needle.dispose();
        dial.setColor(new Color(68,48,29));
        dial.fillOval(-6,-6,12,12);
        dial.setPaint(new GradientPaint(-4,-5,new Color(244,210,143),4,5,new Color(101,71,44)));
        dial.fillOval(-4,-5,9,9);
    }

    private static void drawInstruction(Graphics2D g, String text, boolean stable, boolean onMark) {
        g.setFont(Brass.font(Font.BOLD, 13));
        int w = g.getFontMetrics().stringWidth(text) + 39;
        int x = (WIDTH - w)/2, y = 199;
        g.setColor(new Color(26, 20, 13, 170));
        g.fillRoundRect(x+1,y+2,w,26,8,8);
        g.setPaint(new GradientPaint(0,y,new Color(161,128,78),0,y+25,new Color(83,62,40)));
        g.fillRoundRect(x,y,w,25,8,8);
        g.setPaint(new GradientPaint(0,y,new Color(47,63,53),0,y+24,new Color(23,37,32)));
        g.fillRoundRect(x+1,y+1,w-2,23,7,7);
        g.setColor(new Color(17, 22, 18));
        g.fillOval(x+8, y+6, 13, 13);
        g.setPaint(new GradientPaint(0,y+7,stable || onMark ? new Color(169,231,160) : new Color(245,188,99),
                0,y+17,stable || onMark ? new Color(69,125,85) : new Color(148,88,37)));
        g.fillOval(x+10,y+8,9,9);
        g.setColor(IVORY);
        g.drawString(text, x+27, y+17);
    }

    private static void centered(Graphics2D g, String s, int cx, int baseline) {
        g.drawString(s, cx-g.getFontMetrics().stringWidth(s)/2, baseline);
    }

    private static BufferedImage steamPuff() {
        BufferedImage puff = new BufferedImage(48,48,BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g=puff.createGraphics();
        g.setPaint(new RadialGradientPaint(24,24,24,new float[]{0,.38f,1},
                new Color[]{new Color(249,244,222,160),new Color(227,224,206,100),new Color(220,221,207,0)}));
        g.fillOval(0,0,48,48);
        g.dispose();
        return puff;
    }

    private static void drawSteam(Graphics2D g, double phase) {
        for (int i=0; i<11; i++) {
            double t=(phase-i*.024)/.77;
            if (t<=0 || t>=1) continue;
            int x=(int)(18+58*t+6*t*Math.sin(i*2.4+t*5));
            int y=(int)(200-33*t*t+7*Math.sin(i+t*3)*t);
            int size=(int)(7+22*t);
            g.setComposite(AlphaComposite.SrcOver.derive((float)(.8*(1-t))));
            g.drawImage(MIST, x-size/2, y-size/2, size,size,null);
        }
        g.setComposite(AlphaComposite.SrcOver);
    }
}
