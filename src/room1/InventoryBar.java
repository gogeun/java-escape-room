package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import javax.swing.*;
import room1.RoomProgress.Item;

/** Eight square sockets, set into a freestanding tool rack. */
final class InventoryBar extends JPanel {
    private static final int SLOTS = 8, GAP = 3, WING = 25, PAD_Y = 8;
    private final AbstractButton[] buttons = new AbstractButton[SLOTS];
    private final Item[] items = new Item[SLOTS];
    private final Timer glint = new Timer(1000 / 30, e -> updateGlint());
    private int count;
    private long pickedUp;
    private BufferedImage frame;

    InventoryBar(Consumer<Item> inspect) {
        super(null);
        setOpaque(false);
        getAccessibleContext().setAccessibleName("인벤토리");
        setPreferredSize(sizeFor(1080));
        for (int i = 0; i < SLOTS; i++) {
            final int index = i;
            JButton button = new JButton() {
                @Override protected void paintComponent(Graphics gr) {
                    Item item = items[index];
                    if (item == null) return;
                    Graphics2D g = (Graphics2D)gr.create();
                    Brass.smooth(g);
                    int s = getWidth();
                    boolean hover = getModel().isRollover() || hasFocus();
                    if (hover) {
                        g.setPaint(new GradientPaint(0, 3, new Color(179, 137, 72, 85),
                                s, s, new Color(211, 161, 74, 26)));
                        g.fillRect(3, 3, s-6, s-6);
                        g.setColor(HudMetal.LIGHT);
                        g.drawRect(2, 2, s-5, s-5);
                    }
                    int art = s-8;
                    drawItem(g, item, (s-art)/2, (s-art)/2, art);
                    if (index == count-1 && glint.isRunning()) {
                        float t = Math.min(1f, (System.nanoTime()-pickedUp)/520_000_000f);
                        g.setColor(new Color(251, 220, 137, (int)(140 * (1-t))));
                        g.drawRect(2, 2, s-5, s-5);
                        g.clipRect(3, 3, s-6, s-6);
                        int x = (int)((s * 2) * t);
                        g.setColor(new Color(247, 224, 157, (int)(65 * (1-t))));
                        g.fill(HudMetal.polygon(x-5,0, x,0, x-s,s, x-s-5,s));
                    }
                    g.dispose();
                }
            };
            button.setOpaque(false);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setRolloverEnabled(true);
            button.setEnabled(false);
            button.getAccessibleContext().setAccessibleName("빈 칸 " + (i+1));
            button.addActionListener(e -> inspect.accept(items[index]));
            buttons[i] = button;
            add(button);
        }
    }

    Dimension sizeFor(int stageWidth) {
        int cell = Math.max(34, Math.min(52, Math.round(stageWidth * .043f)));
        return new Dimension(SLOTS * cell + (SLOTS-1)*GAP + 2*WING, cell + 2*PAD_Y + 5);
    }

    private int cellSize() {
        return Math.max(1, Math.min((getWidth()-2*WING-(SLOTS-1)*GAP)/SLOTS, getHeight()-2*PAD_Y-5));
    }

    @Override public void doLayout() {
        int s = cellSize();
        int x = (getWidth() - SLOTS*s - (SLOTS-1)*GAP)/2;
        for (AbstractButton button : buttons) {
            button.setBounds(x, PAD_Y, s, s);
            x += s + GAP;
        }
    }

    private Shape contour() {
        int w = getWidth()-1, h = getHeight()-4;
        return HudMetal.polygon(1, 9, 7, 4, 17, 11, 21, 5, 26, 2, w-26, 2,
                w-21, 5, w-17, 11, w-7, 4, w-1, 9, w-5, h-12,
                w-13,h-5, w-24,h-6, w-31,h, 31,h, 24,h-6, 13,h-5, 5,h-12);
    }

    @Override public boolean contains(int x, int y) { return contour().contains(x, y); }

    @Override protected void paintComponent(Graphics graphics) {
        if (getWidth() < 2 || getHeight() < 2) return;
        if (frame == null || frame.getWidth() != getWidth() || frame.getHeight() != getHeight()) {
            frame = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = frame.createGraphics();
            Brass.smooth(g);
            int w = getWidth(), h = getHeight();
            int s = cellSize();
            int x = (w - SLOTS*s - (SLOTS-1)*GAP)/2;
            HudMetal.chassis(g, contour());
            HudMetal.enamel(g, HudMetal.plate(16,5,w-33,h-10,7));
            g.setColor(new Color(213, 176, 110));
            g.drawLine(x+3, 4, w-x-4, 4);
            for (int i = 0; i < SLOTS; i++) drawSocket(g, x + i*(s+GAP), PAD_Y, s);
            wing(g, h);
            Graphics2D mirror = (Graphics2D)g.create();
            mirror.translate(w-1, 0);
            mirror.scale(-1,1);
            wing(mirror, h);
            mirror.dispose();
            int railY = PAD_Y+s+3;
            g.setColor(new Color(13, 22, 22));
            g.fillRect(x, railY, w-2*x, 4);
            g.setColor(new Color(103, 151, 129));
            g.drawLine(x+1, railY+1, w-x-2, railY+1);
            for (int i=1; i<SLOTS; i++) {
                int join = x + i*(s+GAP)-3;
                g.setColor(HudMetal.BRASS);
                g.drawLine(join, railY, join, railY+2);
            }
            g.dispose();
        }
        graphics.drawImage(frame, 0, 0, null);
    }

    private static void drawSocket(Graphics2D g, int x, int y, int s) {
        g.setColor(new Color(13, 17, 17));
        g.fillRect(x-1, y-1, s+2, s+2);
        g.setPaint(HudMetal.metal(y,s));
        g.fillRect(x,y,s,s);
        g.setPaint(new GradientPaint(x+3,y+2,new Color(9,19,21),x+s-3,y+s-1,new Color(48,57,48)));
        g.fillRect(x+2, y+2, s-4, s-4);
        g.setColor(new Color(255,237,177,45));
        g.drawLine(x+3, y+s-3, x+s-4, y+s-3);
        g.setColor(new Color(118, 145, 122, 48));
        int cx=x+s/2, cy=y+s/2, r=Math.max(3,s/10);
        Path2D lock = new Path2D.Double();
        lock.moveTo(cx,cy-r); lock.lineTo(cx+r,cy); lock.lineTo(cx,cy+r); lock.lineTo(cx-r,cy); lock.closePath();
        g.draw(lock);
    }

    private static void wing(Graphics2D g, int h) {
        Shape guard = HudMetal.polygon(3,10, 7,7, 16,17, 17,h-16, 12,h-10, 8,h-14);
        g.setPaint(new GradientPaint(3,10,HudMetal.LIGHT,17,h-15,HudMetal.BRONZE));
        g.fill(guard);
        g.setColor(HudMetal.BRONZE.darker());
        g.draw(guard);
        g.setColor(new Color(40,61,55));
        g.fill(HudMetal.polygon(6,12, 12,18, 13,h-22, 9,h-17));
        HudMetal.pin(g, 17, h-13, 2);
    }

    void addItem(Item item) {
        items[count] = item;
        AbstractButton slot = buttons[count++];
        slot.getAccessibleContext().setAccessibleName(item.name);
        slot.setToolTipText(item.name);
        slot.setEnabled(true);
        slot.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pickedUp = System.nanoTime();
        glint.restart();
        slot.repaint();
    }

    private void updateGlint() {
        if (System.nanoTime()-pickedUp >= 520_000_000L) glint.stop();
        if (count>0) buttons[count-1].repaint();
    }

    @Override public void removeNotify() { glint.stop(); super.removeNotify(); }

    static void drawItem(Graphics2D original, Item item, int x, int y, int size) {
        Graphics2D g = (Graphics2D) original.create();
        Brass.smooth(g);
        g.translate(x,y);
        g.scale(size/64.0,size/64.0);
        switch (item) {
            case HANDLE -> {
                g.setColor(new Color(18, 23, 22, 165));
                g.setStroke(new BasicStroke(7));
                g.drawOval(8,10,48,48);
                for (int i=0;i<5;i++) {
                    g.setPaint(new GradientPaint(30,9,HudMetal.LIGHT,36,32,HudMetal.BRONZE));
                    g.fill(HudMetal.polygon(28,33, 30,9, 35,9, 35,32));
                    g.rotate(Math.PI*2/5,32,32);
                }
                g.setPaint(new GradientPaint(9,5,new Color(255,213,139),54,58,new Color(160,93,47)));
                g.setStroke(new BasicStroke(6));
                g.drawOval(8,8,48,48);
                g.setStroke(new BasicStroke(1.4f));
                g.setColor(HudMetal.LIGHT);
                g.drawArc(6,6,52,52,60,105);
                g.setPaint(HudMetal.metal(25,15));
                g.fill(HudMetal.plate(25,25,14,14,4));
                g.setColor(HudMetal.DARK);
                g.fillRect(30,30,5,5);
            }
            case CHART -> {
                g.rotate(-.13,32,32);
                g.setColor(new Color(12,20,19,125));
                g.fillRoundRect(12,9,43,51,5,5);
                g.setPaint(new GradientPaint(12,9,new Color(252,231,172),51,56,new Color(193,149,86)));
                g.fill(HudMetal.polygon(11,9, 46,9, 53,16, 53,55, 49,58, 11,58));
                g.setColor(HudMetal.BRONZE);
                g.drawLine(46,9,46,17); g.drawLine(46,17,52,17);
                g.drawRect(15,14,32,38);
                g.setStroke(new BasicStroke(2));
                g.drawArc(21,23,19,18,8,164); g.drawLine(31,32,36,26);
                g.drawLine(21,41,40,41); g.drawLine(21,46,33,46);
            }
            case TAG -> {
                g.rotate(-.22,32,32);
                g.setColor(new Color(194,181,148));
                g.setStroke(new BasicStroke(3));
                g.drawOval(26,1,12,18);
                g.setPaint(HudMetal.metal(11,48));
                g.fill(HudMetal.plate(12,11,40,47,7));
                g.setColor(HudMetal.BRONZE);
                g.draw(HudMetal.plate(15,14,34,41,5));
                g.setColor(HudMetal.DARK);
                g.fillOval(30,16,5,5);
                g.setStroke(new BasicStroke(2));
                for (int i=0;i<3;i++) g.drawLine(20,29+i*7,44-i*3,29+i*7);
            }
            case TOKEN -> {
                g.setColor(new Color(46,37,23));
                g.fillOval(7,9,51,51);
                g.setPaint(HudMetal.metal(6,50));
                g.fillOval(7,6,50,50);
                g.setStroke(new BasicStroke(2));
                g.setColor(HudMetal.BRONZE);
                g.drawOval(11,10,42,42);
                g.setColor(new Color(46,65,59));
                g.fillOval(15,14,34,34);
                g.setPaint(new GradientPaint(20,17,HudMetal.PAPER,43,50,HudMetal.BRASS));
                g.fill(HudMetal.polygon(17,24, 31,29, 47,23, 40,32, 34,34,
                        39,34, 32,43, 25,35, 30,34, 23,32));
            }
        }
        g.dispose();
    }
}
