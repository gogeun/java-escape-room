package room1;

import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Shared materials for the small pieces of brass hardware mounted over the room. */
final class HudMetal {
    static final Color DARK = new Color(31, 34, 31);
    static final Color BRONZE = new Color(113, 76, 43);
    static final Color BRASS = new Color(195, 150, 85);
    static final Color LIGHT = new Color(247, 213, 148);
    static final Color JADE = new Color(53, 88, 81);
    static final Color PAPER = new Color(244, 228, 192);
    static final Color MINT = new Color(174, 214, 189);
    private static final float[] METAL_STOPS = {0, .09f, .26f, .53f, .69f, .86f, 1};
    private static final Color[] METAL = {new Color(255, 223, 164), new Color(169, 118, 62),
            new Color(204, 161, 97), new Color(111, 76, 43), new Color(183, 135, 73),
            new Color(103, 69, 43), new Color(70, 48, 33)};

    private HudMetal() {}

    static LinearGradientPaint metal(float y, float height) {
        return new LinearGradientPaint(0, y, 0, y + Math.max(height, 1), METAL_STOPS, METAL);
    }

    static Path2D polygon(double... points) {
        Path2D shape = new Path2D.Double();
        shape.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) shape.lineTo(points[i], points[i + 1]);
        shape.closePath();
        return shape;
    }

    static Shape plate(int x, int y, int w, int h, int cut) {
        return polygon(x+cut,y, x+w-cut,y, x+w,y+cut, x+w,y+h-cut,
                x+w-cut,y+h, x+cut,y+h, x,y+h-cut, x,y+cut);
    }

    static void chassis(Graphics2D g, Shape shape) {
        Rectangle bounds = shape.getBounds();
        g.translate(0, 2);
        g.setColor(new Color(8, 11, 12, 154));
        g.setStroke(new BasicStroke(3));
        g.draw(shape);
        g.translate(0, -2);
        g.setPaint(metal(bounds.y, bounds.height));
        g.fill(shape);
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(53, 39, 28));
        g.draw(shape);
    }

    static void enamel(Graphics2D g, Shape shape) {
        Rectangle bounds = shape.getBounds();
        g.setPaint(new GradientPaint(0, bounds.y, new Color(52, 78, 70),
                0, bounds.y + bounds.height, new Color(19, 35, 35)));
        g.fill(shape);
        g.setColor(new Color(35, 37, 30));
        g.draw(shape);
    }

    static void pin(Graphics2D g, int x, int y, int radius) {
        int diameter = radius * 2;
        g.setColor(new Color(25, 28, 24));
        g.fillOval(x-radius-1, y-radius, diameter+2, diameter+2);
        g.setPaint(metal(y-radius, diameter));
        g.fillOval(x-radius, y-radius, diameter, diameter);
        g.setColor(BRONZE.darker());
        g.drawLine(x-radius/2, y+radius/2, x+radius/2, y-radius/2);
    }

    static void crystal(Graphics2D g, int x, int y, int w, int h, Color light, Color dark) {
        Shape socket = new RoundRectangle2D.Double(x, y, w, h, 2, 2);
        g.setColor(DARK);
        g.fill(socket);
        g.setPaint(new GradientPaint(x, y+1, light, x, y+h-1, dark));
        g.fillRoundRect(x+1, y+1, w-2, h-2, 2, 2);
        g.setColor(new Color(255, 248, 210, 105));
        g.drawLine(x+2, y+2, x+w-3, y+2);
    }
}
