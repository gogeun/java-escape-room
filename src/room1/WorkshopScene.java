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
    private static final Rectangle[] AREAS = {
        new Rectangle(8, 280, 320, 320), new Rectangle(326, 228, 124, 141),
        new Rectangle(475, 172, 187, 333), new Rectangle(667, 253, 315, 246),
        new Rectangle(1000, 48, 275, 470)
    };
    private final BufferedImage workshop = RoomMedia.picture("workshop");
    private final BufferedImage castle = RoomMedia.picture("arrival");
    private final EnumMap<Station, JButton> targets = new EnumMap<>(Station.class);
    private double openingProgress, beaconAngle, arrival;
    private int solved;

    WorkshopScene(Consumer<Station> inspect) {
        setLayout(null);
        setBackground(Brass.INK);
        for (Station station : Station.values()) {
            JButton b = new JButton(station.name) {
                @Override protected void paintComponent(Graphics graphics) {
                    boolean active = getModel().isRollover() || isFocusOwner();
                    Graphics2D g = (Graphics2D) graphics.create();
                    Brass.smooth(g);
                    int cx = getWidth()/2, cy = getHeight()/2;
                    g.setColor(new Color(27, 20, 13, active ? 225 : 160));
                    g.fillOval(cx-12, cy-12, 24, 24);
                    g.setColor(active ? Color.WHITE : Brass.GOLD);
                    g.drawOval(cx-12, cy-12, 24, 24);
                    g.setStroke(new BasicStroke(1.6f));
                    g.drawOval(cx-5, cy-5, 7, 7);
                    g.drawLine(cx+1, cy+1, cx+5, cy+5);
                    if (active) {
                        g.setFont(Brass.font(Font.BOLD, 14));
                        int width = g.getFontMetrics().stringWidth(getText()) + 14;
                        g.setColor(Brass.INK);
                        g.fillRoundRect(cx-width/2, cy+16, width, 25, 7, 7);
                        g.setColor(Brass.CREAM);
                        g.drawString(getText(), cx-width/2+7, cy+34);
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

    void update(double openingFraction, int completed, double arrivalFraction, double elapsedSeconds) {
        if (completed < 3) {
            double period = completed < 2 ? 2.8 : 4.2;
            beaconAngle = (beaconAngle + 2 * Math.PI * elapsedSeconds / period) % (2 * Math.PI);
        } else if (openingProgress == openingFraction && solved == completed && arrival == arrivalFraction) {
            return;
        }
        openingProgress = openingFraction;
        solved = completed;
        arrival = arrivalFraction;
        repaint();
    }

    private double scale() {
        return Math.min(getWidth()/1280.0, getHeight()/720.0);
    }

    @Override public void doLayout() {
        double k = scale();
        int x = (int)((getWidth() - 1280*k)/2), y = (int)((getHeight() - 720*k)/2);
        for (Station station : Station.values()) {
            Rectangle r = AREAS[station.ordinal()];
            targets.get(station).setBounds(x + (int)(r.x*k), y + (int)(r.y*k),
                    (int)(r.width*k), (int)(r.height*k));
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        Brass.smooth(g);
        double k = scale();
        g.translate((getWidth()-1280*k)/2, (getHeight()-720*k)/2);
        g.scale(k, k);
        g.clipRect(0, 0, 1280, 720);
        if (openingProgress < 1) {
            // 짧게 치고 들어와 마지막 20%에서 감쇠한다. 확대 여백은 흔들릴 때만 쓴다.
            double strength = Math.min(1, openingProgress * 28) * Math.min(1, (1 - openingProgress) * 5);
            double swell = 0.76 + 0.24 * Math.cos(openingProgress * Math.PI * 10);
            double zoom = 1 + 0.012 * strength;
            g.translate(640, 360);
            g.scale(zoom, zoom);
            g.translate(-640 + 5.4 * strength * swell * Math.sin(openingProgress * 116),
                        -360 + 3.9 * strength * swell * Math.sin(openingProgress * 153));
        }
        g.drawImage(workshop, -2, -2, 1284, 724, null);
        if (solved < 3) {
            float intensity = solved < 2 ? 1f : 0.42f;
            drawBeacon(g, 454, 181, beaconAngle, intensity);
            drawBeacon(g, 688, 202, beaconAngle + 0.5, intensity);
            drawBeacon(g, 987, 203, beaconAngle + 1.0, intensity);
        }
        if (solved >= 2) {
            g.setColor(new Color(222, 195, 146));
            g.fillOval(514, 208, 87, 87);
            drawDial(g, 558, 252, 41, 50);
        }
        if (arrival > 0) {
            // 철문이 닫혀 암전된 뒤, 과거의 성이 드러난다.
            if (arrival >= .5) g.drawImage(castle, -2, -2, 1284, 724, null);
            double cover = 1 - Math.abs(2*arrival - 1);
            int shutter = (int)Math.ceil(640 * cover);
            g.setColor(new Color(29, 24, 20));
            g.fillRect(0, 0, shutter, 720);
            g.fillRect(1280-shutter, 0, shutter, 720);
            g.setColor(Brass.EDGE);
            g.drawLine(shutter, 0, shutter, 720);
            g.drawLine(1279-shutter, 0, 1279-shutter, 720);
        }
        g.dispose();
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
