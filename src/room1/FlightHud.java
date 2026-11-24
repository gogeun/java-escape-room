package room1;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

/** Separate pieces of cockpit instrumentation. Unpainted areas forward hits to the scene. */
final class FlightHud extends JPanel {
    private final PilotPlate pilot;
    private final Chronometer chronometer = new Chronometer();
    private final CommandButton messages, settings;

    FlightHud(String passenger, Runnable openMessages, Runnable openSettings) {
        super(null);
        setOpaque(false);
        pilot = new PilotPlate(passenger);
        messages = new CommandButton("메시지", false, openMessages);
        settings = new CommandButton("설정", true, openSettings);
        add(pilot);
        add(chronometer);
        add(messages);
        add(settings);
        setVitals(3,0);
    }

    void setVitals(int hearts, int solved) { pilot.setVitals(hearts,solved); }
    void setTime(String time) { chronometer.readout.setText(time); }

    @Override public void doLayout() {
        boolean compact = getWidth() < 860;
        int margin = compact ? 8 : 16, y = compact ? 6 : 11;
        int buttonW = compact ? 39 : 75;
        int right = getWidth()-margin;
        pilot.setBounds(margin, y, compact ? 287 : 301, 62);
        settings.setBounds(right-buttonW,y+3,buttonW,43);
        right -= buttonW+4;
        messages.setBounds(right-buttonW,y+3,buttonW,43);
        right -= buttonW+9;
        chronometer.setBounds(right-96,y+3,96,43);
    }

    @Override public boolean contains(int x, int y) {
        for (Component part : getComponents()) {
            if (part.isVisible() && part.contains(x-part.getX(),y-part.getY())) return true;
        }
        return false;
    }

    private static final class PilotPlate extends JPanel {
        private final JLabel title = Brass.label("공중도시의 시간 기관실",14,HudMetal.PAPER,true);
        private final JLabel passenger;
        private final JLabel hearts = new JLabel() {
            @Override protected void paintComponent(Graphics gr) {
                Graphics2D g=(Graphics2D)gr.create();
                Brass.smooth(g);
                for (int i=0;i<3;i++) paintHeart(g,4+i*16,4,i<vitality);
                g.dispose();
            }
        };
        private final JLabel status = Brass.label("",12,HudMetal.MINT,false);
        private int vitality, completed;
        private BufferedImage mount;

        PilotPlate(String name) {
            super(null);
            setOpaque(false);
            passenger=Brass.label("",13,new Color(224,210,176),false);
            passenger.putClientProperty("html.disable",Boolean.TRUE);
            passenger.setText(name);
            status.setHorizontalAlignment(SwingConstants.RIGHT);
            add(title); add(passenger); add(hearts); add(status);
        }

        void setVitals(int lives, int solved) {
            vitality=lives;
            completed=solved;
            hearts.setText("♥".repeat(lives)+"♡".repeat(3-lives));
            hearts.getAccessibleContext().setAccessibleName("체력 "+lives+" / 3");
            hearts.setToolTipText("체력 "+lives+" / 3");
            String condition = solved==0 ? "기관 폭주" : solved==1 ? "압력 안정" : "기관 정지";
            status.setText(condition+"   "+solved+" / 2");
            status.setForeground(solved==0 ? new Color(249,193,117) : HudMetal.MINT);
            repaint();
        }

        @Override public void doLayout() {
            int w=getWidth(), start=60;
            title.setBounds(start,7,w-start-60,21);
            passenger.setBounds(start,28,w-start-107,19);
            hearts.setBounds(w-64,8,51,18);
            status.setBounds(w-112,28,95,19);
        }

        private Shape shape() {
            int w=getWidth();
            return HudMetal.polygon(3,16, 15,3, 41,3, 47,6, w-16,6, w-5,12,
                    w-7,40, w-18,51, 50,51, 41,57, 18,57, 3,44);
        }

        @Override public boolean contains(int x,int y) { return shape().contains(x,y); }

        @Override protected void paintComponent(Graphics gr) {
            int w=getWidth();
            if (w<65) return;
            if (mount==null || mount.getWidth()!=w) {
                mount=new BufferedImage(w,62,BufferedImage.TYPE_INT_ARGB);
                Graphics2D g=mount.createGraphics();
                Brass.smooth(g);
                HudMetal.chassis(g,shape());
                Shape face=HudMetal.polygon(6,18, 17,7, 41,7, 46,9, w-17,9, w-9,14,
                        w-11,39, w-20,48, 49,48, 40,53, 19,53, 6,42);
                HudMetal.enamel(g,face);
                g.setColor(new Color(8,22,23,124));
                g.drawLine(62,28,w-18,28);
                g.setColor(new Color(247,222,154,20));
                g.drawLine(62,29,w-18,29);
                g.setColor(new Color(237,201,136,155));
                g.drawLine(57,8,w-20,8);
                drawCompass(g);
                g.dispose();
            }
            gr.drawImage(mount,0,0,null);
            Graphics2D g=(Graphics2D)gr.create();
            Brass.smooth(g);
            for (int i=0;i<2;i++) {
                Color top = i<completed ? new Color(166,222,171) : new Color(74,90,74);
                Color bottom = i<completed ? new Color(57,129,102) : new Color(37,49,43);
                HudMetal.crystal(g,w-67+i*21,47,19,5,top,bottom);
            }
            g.dispose();
        }

        private static void drawCompass(Graphics2D g) {
            g.setColor(new Color(21,33,31));
            g.fillOval(6,6,49,49);
            g.setPaint(HudMetal.metal(7,45));
            g.fillOval(7,7,46,46);
            g.setColor(new Color(91,68,42));
            g.drawOval(9,9,42,42);
            g.setPaint(new RadialGradientPaint(26,23,25,new float[]{0,1},
                    new Color[]{new Color(73,108,94),new Color(22,46,44)}));
            g.fillOval(11,11,38,38);
            g.translate(30,30);
            g.setColor(new Color(186,164,108));
            for (int i=0;i<12;i++) {
                g.drawLine(0,-17,0,i%3==0?-13:-15);
                g.rotate(Math.PI/6);
            }
            g.rotate(-.30);
            for (int i=0;i<4;i++) {
                int reach=i%2==0?15:10;
                g.setColor(i==0 ? HudMetal.PAPER : HudMetal.BRASS);
                g.fill(HudMetal.polygon(0,-reach,0,1,-4,0));
                g.setColor(i==0?new Color(207,143,68):HudMetal.BRONZE);
                g.fill(HudMetal.polygon(0,-reach,4,0,0,1));
                g.rotate(Math.PI/2);
            }
            g.setColor(HudMetal.LIGHT);
            g.fillOval(-2,-2,4,4);
        }

        private static void paintHeart(Graphics2D parent,int x,int y,boolean live) {
            Graphics2D g=(Graphics2D)parent.create();
            g.translate(x,y);
            Path2D heart=new Path2D.Double();
            heart.moveTo(7,3);
            heart.curveTo(2,-1,-1,3,1,7);
            heart.curveTo(2,9,5,12,7,14);
            heart.curveTo(9,12,13,9,14,6);
            heart.curveTo(15,1,10,0,7,3);
            heart.closePath();
            Color top=live?new Color(237,138,102):new Color(75,84,74);
            Color bottom=live?new Color(146,55,44):new Color(40,52,49);
            g.setPaint(new GradientPaint(0,1,top,0,14,bottom));
            g.fill(heart);
            g.setColor(live?new Color(250,192,137):new Color(120,127,110));
            g.setStroke(new BasicStroke(.7f));
            g.draw(heart);
            if (live) g.drawLine(2,4,4,3);
            g.dispose();
        }
    }

    private static final class Chronometer extends JPanel {
        final JLabel readout=Brass.label("00:00",15,HudMetal.PAPER,true);
        Chronometer() {
            super(null);
            setOpaque(false);
            setToolTipText("기관실 경과 시간");
            readout.setFont(new Font(Font.MONOSPACED,Font.BOLD,15));
            readout.setHorizontalAlignment(SwingConstants.CENTER);
            add(readout);
        }
        @Override public void doLayout() { readout.setBounds(28,4,getWidth()-36,32); }
        @Override public boolean contains(int x,int y) {
            return HudMetal.plate(2,2,getWidth()-5,getHeight()-6,8).contains(x,y);
        }
        @Override protected void paintComponent(Graphics gr) {
            int w=getWidth(),h=getHeight();
            Graphics2D g=(Graphics2D)gr.create();
            Brass.smooth(g);
            HudMetal.chassis(g,HudMetal.plate(2,2,w-5,h-6,8));
            HudMetal.enamel(g,HudMetal.plate(5,5,w-11,h-12,6));
            g.setPaint(HudMetal.metal(11,19));
            g.fillOval(10,11,20,20);
            g.setColor(new Color(36,49,43));
            g.fillOval(12,13,16,16);
            g.setColor(HudMetal.PAPER);
            g.setStroke(new BasicStroke(1.3f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            g.drawLine(20,21,20,15);
            g.drawLine(20,21,24,23);
            g.setColor(HudMetal.BRASS);
            g.drawLine(18,9,22,9);
            g.dispose();
        }
    }

    private static final class CommandButton extends JButton {
        final boolean gear;
        CommandButton(String label, boolean gear, Runnable action) {
            super(label);
            this.gear=gear;
            setOpaque(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setRolloverEnabled(true);
            setFont(Brass.font(Font.BOLD,12));
            setToolTipText(label);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addActionListener(e->action.run());
        }
        @Override public boolean contains(int x,int y) {
            return HudMetal.plate(2,2,getWidth()-5,getHeight()-6,6).contains(x,y);
        }
        @Override protected void paintComponent(Graphics gr) {
            int w=getWidth(),h=getHeight();
            Graphics2D g=(Graphics2D)gr.create();
            Brass.smooth(g);
            boolean lit=getModel().isRollover()||isFocusOwner();
            HudMetal.chassis(g,HudMetal.plate(2,2,w-5,h-6,6));
            Shape face=HudMetal.plate(5,5,w-11,h-12,4);
            HudMetal.enamel(g,face);
            if (lit || getModel().isArmed()) {
                g.setColor(getModel().isArmed()?new Color(3,15,14,125):new Color(224,192,126,39));
                g.fill(face);
                g.setColor(HudMetal.LIGHT);
                g.draw(face);
            }
            boolean withText=w>=58;
            g.setColor(lit?HudMetal.PAPER:HudMetal.LIGHT);
            g.setStroke(new BasicStroke(1.5f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
            int cx=withText?18:w/2, cy=h/2-1;
            if (gear) paintGear(g,cx,cy);
            else {
                g.drawRoundRect(cx-8,cy-5,16,11,2,2);
                g.drawLine(cx-7,cy-4,cx,cy+1);
                g.drawLine(cx,cy+1,cx+7,cy-4);
            }
            if (withText) {
                g.setFont(getFont());
                g.setColor(HudMetal.PAPER);
                int fw=g.getFontMetrics().stringWidth(getText());
                g.drawString(getText(),29+(w-36-fw)/2,cy+4);
            }
            g.dispose();
        }
        private static void paintGear(Graphics2D g,int x,int y) {
            Graphics2D teeth=(Graphics2D)g.create();
            teeth.translate(x,y);
            for (int i=0;i<8;i++) {
                teeth.fillRoundRect(-1,-9,3,4,1,1);
                teeth.rotate(Math.PI/4);
            }
            teeth.setStroke(new BasicStroke(2.7f));
            teeth.drawOval(-6,-6,12,12);
            teeth.setStroke(new BasicStroke(1.3f));
            teeth.drawOval(-2,-2,4,4);
            teeth.dispose();
        }
    }
}
