package room3;

import java.awt.*;
import javax.swing.*;

/** Temporary text-free mode. Restore text with -Droom3.showText=true. */
public final class TextDisplay {
    public static final boolean ENABLED=Boolean.getBoolean("room3.showText");
    private TextDisplay() {}
    public static String text(String value) { return ENABLED?value:""; }
    public static String tooltip(String value) { return ENABLED?value:null; }
    public static JLabel label(String value) { return label(value,SwingConstants.LEADING); }
    public static JLabel label(String value,int alignment) {
        return new JLabel(value,alignment) {
            @Override public void setText(String next) { super.setText(text(next)); }
        };
    }
    public static Icon icon(String action) {
        if(ENABLED) return null;
        int kind=switch(action) {
            case "닫기" -> 0;
            case "← 이전", "← 예배당으로 돌아가기" -> 1;
            case "다음 →" -> 2;
            case "되돌리기" -> 3;
            default -> -1;
        };
        if(kind<0) return null;
        return new Icon() {
            public int getIconWidth() {return 22;}
            public int getIconHeight() {return 22;}
            public void paintIcon(Component c,Graphics graphics,int x,int y) {
                Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(205,165,89));g.setStroke(new BasicStroke(2));
                if(kind==0) {g.drawLine(5,5,17,17);g.drawLine(5,17,17,5);}
                else if(kind==3) {g.drawArc(4,4,14,14,35,290);g.drawLine(17,4,17,10);g.drawLine(17,10,11,9);}
                else {int end=kind==1?4:18,other=kind==1?10:12;g.drawLine(4,11,18,11);g.drawLine(end,11,other,5);g.drawLine(end,11,other,17);}
                g.dispose();
            }
        };
    }
}
