package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.function.Consumer;
import javax.swing.*;
import room1.RoomProgress.Station;

/** 배경과 클릭 영역이 같은 1280 × 720 좌표계를 쓴다. */
final class WorkshopScene extends JPanel {
    private static Rectangle area(Station station) {
        return switch (station) {
            case DESK -> new Rectangle(8, 326, 323, 284);
            case WINDOW -> new Rectangle(122, 76, 179, 201);
            case TUBE -> new Rectangle(332, 244, 111, 122);
            case PRESSURE -> new Rectangle(477, 184, 164, 303);
            case STOP -> new Rectangle(669, 266, 300, 215);
            case LAMP -> new Rectangle(907, 87, 95, 147);
            case LIFT -> new Rectangle(1010, 83, 249, 427);
        };
    }
    private final BufferedImage workshop = WorkshopView.backdrop();
    private BufferedImage departureStill;
    private final BufferedImage magnifier = RoomMedia.picture("magnifier.png");
    private final BufferedImage magnifierHover = RoomMedia.picture("magnifier-hover.png");
    private final BufferedImage speakingTube = RoomMedia.picture("tube-mount.png");
    private final BufferedImage controlPlate = RoomMedia.picture("shutdown-face.jpg");
    private static final int[] CONTACT_STEPS = {2, 4, 1, 3};
    private static final Polygon CONSOLE_FACE = new Polygon(
            new int[]{694, 952, 965, 684}, new int[]{339, 339, 390, 390}, 4);
    private final EnumMap<Station, JButton> targets = new EnumMap<>(Station.class);
    private double openingProgress, beaconAngle, arrival, steamTime;
    private double impactPhase, impactStrength;
    private int solved, stopStep;
    private boolean handleInstalled;
    private double handleTurns;
    private final BufferedImage steam = steamPuff();

    WorkshopScene(Consumer<Station> inspect) {
        setLayout(null);
        setBackground(Brass.INK);
        for (Station station : Station.values()) {
            JButton b = new JButton(station.name) {
                @Override protected void paintComponent(Graphics graphics) {
                    boolean active = getModel().isRollover() || isFocusOwner();
                    Graphics2D g = (Graphics2D) graphics.create();
                    Brass.smooth(g);
                    float alpha = (float)Math.min(1, openingProgress / .48);
                    g.setComposite(AlphaComposite.SrcOver.derive(alpha));
                    int cx = getWidth()/2, cy = getHeight()/2;
                    if (station == Station.PRESSURE) {
                        // Keep the wheel rim and its index mark visible below the gauge.
                        cx = getWidth() * 4 / 5;
                        cy = getHeight() * 3 / 7;
                    }
                    g.drawImage(active ? magnifierHover : magnifier, cx-14, cy-14, 28, 28, null);
                    if (active) {
                        g.setFont(Brass.font(Font.BOLD, 14));
                        int width = g.getFontMetrics().stringWidth(getText()) + 14;
                        int left = Math.max(0, Math.min(getWidth()-width, cx-width/2));
                        g.setColor(Brass.INK);
                        g.fillRoundRect(left, cy+16, width, 25, 7, 7);
                        g.setColor(Brass.CREAM);
                        g.drawString(getText(), left+7, cy+34);
                    }
                    g.dispose();
                }
            };
            b.setToolTipText(station.name);
            b.setContentAreaFilled(false);
            b.setBorderPainted(false);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            b.addActionListener(e -> inspect.accept(station));
            targets.put(station, b);
            add(b);
        }
    }

    void targetsVisible(boolean visible) {
        targets.values().forEach(b -> b.setVisible(visible));
    }

    void setStopStep(int step) {
        if (step == stopStep) return;
        stopStep = step;
        repaint();
    }

    void setValve(boolean installed, double turns) {
        if (handleInstalled == installed && handleTurns == turns) return;
        handleInstalled = installed;
        handleTurns = turns;
        repaint();
    }

    void setImpact(double phase, double strength) {
        impactPhase = phase;
        impactStrength = strength;
    }

    void update(double openingFraction, int completed, double arrivalFraction, double elapsedSeconds) {
        if (completed < 2) {
            double period = completed == 0 ? 2.8 : 4.2;
            beaconAngle = (beaconAngle + 2 * Math.PI * elapsedSeconds / period) % (2 * Math.PI);
            steamTime = (steamTime + elapsedSeconds) % 900;
        } else if (openingProgress == openingFraction && solved == completed && arrival == arrivalFraction) {
            return;
        }
        openingProgress = openingFraction;
        solved = completed;
        arrival = arrivalFraction;
        repaint();
    }

    @Override public void doLayout() {
        Rectangle frame = WorkshopView.bounds(getWidth(), getHeight());
        double k = frame.width / 1280d;
        for (Station station : Station.values()) {
            Rectangle r = area(station);
            targets.get(station).setBounds(frame.x + (int)(r.x*k), frame.y + (int)(r.y*k),
                    (int)(r.width*k), (int)(r.height*k));
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        Brass.smooth(g);
        Rectangle frame = WorkshopView.bounds(getWidth(), getHeight());
        if (arrival > 0) frame = LiftJourney.frame(frame, getWidth(), getHeight(), arrival);
        if (frame.width < getWidth() || frame.height < getHeight()) {
            g.drawImage(workshop, 0, 0, getWidth(), getHeight(), null);
            g.setColor(new Color(14, 20, 18, 155));
            g.fillRect(0, 0, getWidth(), getHeight());
        }
        g.translate(frame.x, frame.y);
        g.scale(frame.width / 1280d, frame.height / 720d);
        g.clipRect(0, 0, 1280, 720);
        if (arrival > 0) {
            if (departureStill == null) {
                departureStill = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
                Graphics2D still = departureStill.createGraphics();
                Brass.smooth(still);
                drawWorkshop(still);
                still.dispose();
            }
            LiftJourney.paint(g, departureStill, arrival);
        } else {
            drawWorkshop(g);
        }
        g.dispose();
    }

    private void drawWorkshop(Graphics2D g) {
        if (impactStrength > 0) {
            double zoom = 1 + .015 * impactStrength;
            g.translate(640, 360);
            g.scale(zoom, zoom);
            g.translate(-640 + 7 * impactStrength * Math.sin(impactPhase * 43),
                        -360 + 4.7 * impactStrength * Math.sin(impactPhase * 56));
        }
        g.drawImage(workshop, 0, 0, 1280, 720, null);
        if (handleInstalled) PressureInstrument.drawMountedWheel(g, 557.5, 382, 54, handleTurns);
        Composite opaque = g.getComposite();
        if (openingProgress < .3) g.setComposite(AlphaComposite.SrcOver.derive((float)(openingProgress / .3)));
        g.drawImage(speakingTube, 337, 247, 93, 87, null);
        drawLamp(g);
        drawPanelControls(g);
        if (solved < 2) {
            float intensity = solved == 0 ? 1f : 0.42f;
            if (solved == 0) drawBeacon(g, 454, 181, beaconAngle, intensity);
            else darkLens(g,454,181);
            drawBeacon(g, 688, 202, beaconAngle + 0.5, intensity);
            drawBeacon(g, 987, 203, beaconAngle + 1.0, intensity);
            drawSteam(g);
        } else {
            darkLens(g,454,181); darkLens(g,688,202); darkLens(g,987,203);
        }
        if (solved == 0) {
            g.setColor(new Color(218, 234, 235, 31));
            g.fillRoundRect(151, 101, 93, 89, 6, 6);
        }
        if (solved >= 1) {
            g.setColor(new Color(222, 195, 146));
            g.fillOval(514, 208, 87, 87);
            drawDial(g, 558, 252, 41, 50);
        }
        g.setComposite(opaque);
    }

    private static BufferedImage steamPuff() {
        BufferedImage cloud = new BufferedImage(96, 72, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = cloud.createGraphics();
        g.scale(1.33, 1);
        g.setPaint(new RadialGradientPaint(36, 36, 36, new float[]{0,.36f,1},
                new Color[]{new Color(235,224,202,153),new Color(224,220,206,94),new Color(226,223,214,0)}));
        g.fillOval(0, 0, 72, 72);
        g.dispose();
        return cloud;
    }

    private void drawSteam(Graphics2D g) {
        for (int n=0; n<12; n++) {
            double t=(steamTime*.42+n/12.0)%1;
            int w=(int)(22+100*t);
            int x=(int)(956-27*t+10*Math.sin(t*6+n*.7));
            int y=(int)(244-122*t);
            Graphics2D puff=(Graphics2D)g.create();
            float opacity=(float)Math.sin(t*Math.PI)*(solved==0 ? .77f : .11f);
            puff.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f,opacity)));
            puff.drawImage(steam,x-w/2,y-w/3,w,w*2/3,null);
            puff.dispose();
        }
    }

    private static void drawLamp(Graphics2D g) {
        g.setStroke(new BasicStroke(4));
        g.setColor(new Color(52,39,26));
        g.drawLine(986,112,956,112); g.drawLine(956,112,956,133);
        g.setColor(new Color(170,131,75));
        g.drawLine(985,109,955,109);
        g.setPaint(new GradientPaint(940,131,new Color(204,166,91),971,158,new Color(73,49,29)));
        g.fillArc(936,129,40,38,0,180);
        g.setColor(new Color(242,213,153));
        g.fillOval(939,145,34,5);
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(119,87,44));
        g.drawLine(971,149,971,175);
        g.setColor(new Color(206,163,87)); g.drawOval(968,174,6,9);
    }

    private static void darkLens(Graphics2D g,int x,int y) {
        g.setPaint(new GradientPaint(x-8,y,new Color(85,52,34),x+8,y,new Color(121,83,47)));
        g.fillRoundRect(x-8,y-7,16,15,4,4);
        g.setColor(new Color(179,133,75));g.drawLine(x-5,y-5,x-5,y+4);
    }

    private void drawPanelControls(Graphics2D g) {
        Shape clip = g.getClip();
        g.clip(CONSOLE_FACE);
        g.drawImage(controlPlate, 684, 339, 281, 51, null);
        g.setClip(clip);
        g.setColor(new Color(121, 85, 42));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(CONSOLE_FACE);
        for (int i=0; i<CONTACT_STEPS.length; i++) {
            int x = 738 + i*64;
            boolean engaged = stopStep >= CONTACT_STEPS[i];
            g.setColor(new Color(52, 43, 29));
            g.fillOval(x-1, 383, 7, 5);
            g.setColor(engaged ? new Color(105, 211, 149) : new Color(139, 104, 57));
            g.fillOval(x, 383, 5, 3);
        }
    }

    /** 황동 경광등의 렌즈 안에서 반사경이 돌아가며 기구 주변에만 붉은빛을 비춘다. */
    private static void drawBeacon(Graphics2D graphics, int x, int y, double angle, float intensity) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setComposite(AlphaComposite.SrcOver.derive(intensity));
        double direction = Math.sin(angle);
        float facing = (float) ((1 + Math.cos(angle)) / 2);
        Graphics2D glow = (Graphics2D) g.create();
        glow.translate(14 * direction, 0);
        glow.scale(1.45, 0.70);
        glow.setPaint(new RadialGradientPaint(0, 0, 34, new float[]{0, 0.45f, 1},
                new Color[]{new Color(255, 54, 28, (int)(80 + 95 * facing)),
                            new Color(238, 33, 16, (int)(34 + 55 * facing)), new Color(225, 35, 15, 0)}));
        glow.fillOval(-34, -34, 68, 68);
        glow.dispose();
        g.setColor(new Color(181, 29, 17, 200));
        g.fillRoundRect(-8, -8, 16, 17, 6, 6);
        g.setColor(new Color(255, 136, 74, (int)(65 + 185 * facing)));
        g.fillRoundRect((int)Math.round(5 * direction) - 2, -6, 4, 13, 3, 3);
        g.dispose();
    }

    static void drawDial(Graphics2D g, int x, int y, int r, double value) {
        g.setColor(Brass.PAPER);
        g.fillOval(x-r, y-r, 2*r, 2*r);
        g.setColor(Brass.BROWN);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x-r, y-r, 2*r, 2*r);
        for (int n=0;n<=100;n+=5) {
            double a=Math.toRadians(145 + n*2.5);
            int length=n%10==0 ? 7 : 4;
            g.draw(new Line2D.Double(x+(r-length)*Math.cos(a),y+(r-length)*Math.sin(a),
                    x+(r-2)*Math.cos(a),y+(r-2)*Math.sin(a)));
        }
        double needle=Math.toRadians(145+value*2.5);
        g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(132, 48, 34));
        g.draw(new Line2D.Double(x,y,x+(r-8)*Math.cos(needle),y+(r-8)*Math.sin(needle)));
        g.setColor(Brass.INK);
        g.fillOval(x-4, y-4, 8, 8);
    }
}
