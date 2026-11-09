package room1;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.JPanel;
import room1.RoomProgress.Station;

/** 확대 화면의 다이얼, 압력계와 작동 표시등. 실제 입력 상태에서 계기 표시를 그린다. */
final class InstrumentStrip extends JPanel {
    private final Station station;
    private final RoomProgress state;
    private String digits = "";
    private double pressure;

    InstrumentStrip(Station station, RoomProgress state, double pressure) {
        this.station = station;
        this.state = state;
        this.pressure = pressure;
        setPreferredSize(new Dimension(260, 96));
        setOpaque(false);
        getAccessibleContext().setAccessibleName(station.name);
    }

    void setDigits(String digits) {
        this.digits = digits;
        repaint();
    }

    void setPressure(double value) {
        if (pressure != value) {
            pressure = value;
            repaint();
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        Brass.smooth(g);
        int w=getWidth(), h=getHeight();
        g.setPaint(new GradientPaint(0,0,new Color(74,60,43),0,h,new Color(40,34,28)));
        g.fillRoundRect(0,0,w-1,h-1,8,8);
        g.setColor(Brass.EDGE);
        g.drawRoundRect(0,0,w-1,h-1,8,8);
        for (int x : new int[]{9,w-10}) {
            for (int y : new int[]{9,h-10}) {
                g.setColor(new Color(182,149,97));
                g.fillOval(x-2,y-2,5,5);
                g.setColor(Brass.BROWN);
                g.drawLine(x-1,y+1,x+1,y-1);
            }
        }
        if (station == Station.PRESSURE) {
            WorkshopScene.drawDial(g,63,h/2,40,pressure);
            g.setFont(Brass.font(Font.PLAIN,14));
            g.setColor(Brass.MUTED);
            g.drawString("증기 압력",126,35);
            g.setColor(pressure <= 50 ? Brass.GREEN : Brass.GOLD);
            g.setFont(new Font(Font.MONOSPACED,Font.BOLD,31));
            g.drawString(String.valueOf(Math.round(pressure)),125,72);
        } else if (station == Station.STOP) {
            g.setFont(Brass.font(Font.PLAIN,14));
            g.setColor(Brass.CREAM);
            g.drawString("정지 절차",25,36);
            g.drawString(state.step()+" / 4",25,67);
            for (int i=0;i<4;i++) {
                int x=139+45*i, y=h/2;
                g.setColor(Brass.INK);
                g.fillOval(x-13,y-13,26,26);
                g.setColor(i<state.step() ? Brass.GREEN : new Color(85,85,65));
                g.fillOval(x-9,y-9,18,18);
                g.setColor(i<state.step() ? new Color(226,243,193) : Brass.EDGE);
                g.drawArc(x-8,y-8,16,16,45,90);
            }
        } else {
            int x=24, y=18;
            boolean clock=station == Station.LIFT;
            for (int i=0;i<4;i++) {
                if (clock && i==2) {
                    g.setColor(Brass.GOLD);
                    g.fillOval(x+1,y+22,4,4);
                    g.fillOval(x+1,y+34,4,4);
                    x+=12;
                }
                g.setPaint(new GradientPaint(0,y,Brass.INK,0,y+31,new Color(87,74,57),true));
                g.fill(new RoundRectangle2D.Double(x,y,37,60,7,7));
                g.setColor(Brass.EDGE);
                g.drawRoundRect(x,y,37,60,7,7);
                g.setFont(new Font(Font.MONOSPACED,Font.BOLD,30));
                g.setColor(Brass.CREAM);
                g.drawString(i<digits.length() ? digits.substring(i,i+1) : "–",x+9,y+40);
                x+=42;
            }
            g.setFont(Brass.font(Font.PLAIN,14));
            g.setColor(Brass.GOLD);
            g.drawString(clock ? "시간 좌표" : "정비함 잠금장치",x+16,45);
            g.setColor(Brass.CREAM);
            g.drawString(clock ? "시 / 분" : "톱니 회전 수",x+16,68);
        }
        g.dispose();
    }
}
