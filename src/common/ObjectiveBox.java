package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 방 화면 오른쪽 위에 떠 있는 "현재 목표" 상자.
 *  - 제목줄을 클릭하면 접기 / 펼치기
 *  - 목표가 바뀌면 테두리가 잠깐 빛난다
 *  - 제목줄 말고는 마우스 클릭이 아래 방 화면으로 그대로 전달된다
 */
public class ObjectiveBox extends JComponent implements GameListener {
    public static final int WIDTH = 262;
    private static final int HEADER = 32, PAD = 12, LINE = 22;

    private String text = "";
    private boolean collapsed;
    private long changedAt;
    private final Timer glow = new Timer(1000 / 30, e -> {
        if (System.currentTimeMillis() - changedAt > 1400) ((Timer) e.getSource()).stop();
        repaint();
    });

    public ObjectiveBox() {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                collapsed = !collapsed;
                SoundManager.getInstance().play(SoundManager.Effect.CLICK);
                relayout();
            }
        });
        setToolTipText("클릭하여 접기 / 펼치기");
        GameManager.getInstance().addListener(this);
    }

    @Override public void onRoomChanged(RoomPanel room, int index, int count) { update(); }
    @Override public void onProgressChanged() { update(); }
    @Override public void onInventoryChanged() { update(); }

    private void update() {
        RoomPanel room = GameManager.getInstance().getCurrentRoom();
        String next = room == null ? "" : room.getObjective();
        if (!next.equals(text)) {
            text = next;
            changedAt = System.currentTimeMillis();
            glow.restart();
        }
        relayout();
    }

    /** 내용 길이에 맞춰 높이를 정하고 오른쪽 위에 붙인다 */
    private void relayout() {
        int h = collapsed ? HEADER + 6 : HEADER + PAD + wrap().size() * LINE + 4;
        setBounds(RoomPanel.ROOM_WIDTH - WIDTH - 12, 10, WIDTH, h);
        repaint();
    }

    private List<String> wrap() {
        List<String> lines = new ArrayList<>();
        FontMetrics fm = getFontMetrics(Theme.font(Font.PLAIN, 16));
        int max = WIDTH - PAD * 2 - 6;
        for (String para : text.split("\n")) {
            StringBuilder cur = new StringBuilder();
            for (String word : para.split(" ")) {
                String tryLine = cur.length() == 0 ? word : cur + " " + word;
                if (fm.stringWidth(tryLine) <= max) {
                    cur = new StringBuilder(tryLine);
                } else {
                    if (cur.length() > 0) lines.add(cur.toString());
                    cur = new StringBuilder(word);
                    while (fm.stringWidth(cur.toString()) > max && cur.length() > 1) {   // 아주 긴 단어
                        int cut = cur.length() - 1;
                        while (cut > 1 && fm.stringWidth(cur.substring(0, cut)) > max) cut--;
                        lines.add(cur.substring(0, cut));
                        cur = new StringBuilder(cur.substring(cut));
                    }
                }
            }
            lines.add(cur.toString());
        }
        return lines;
    }

    /** 제목줄에서만 마우스를 받는다 (나머지는 방 화면으로 통과) */
    @Override public boolean contains(int x, int y) {
        return x >= 0 && x < getWidth() && y >= 0 && y < HEADER + 4;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (text.isEmpty()) return;
        Graphics2D g2 = (Graphics2D) g.create();
        RoomTheme.smooth(g2);
        RoomTheme t = RoomTheme.current();
        int w = getWidth(), h = getHeight();

        t.chassis(g2, RoomTheme.plate(1, 1, w - 3, h - 4, 8));
        Shape inner = RoomTheme.plate(5, 5, w - 11, h - 12, 6);
        Composite old = g2.getComposite();
        g2.setComposite(AlphaComposite.SrcOver.derive(0.93f));
        t.enamel(g2, inner);
        g2.setComposite(old);

        float since = (System.currentTimeMillis() - changedAt) / 1400f;
        if (since < 1) {
            int a = (int) (170 * (1 - since));
            g2.setColor(new Color(t.accentLight.getRed(), t.accentLight.getGreen(), t.accentLight.getBlue(), a));
            g2.setStroke(new BasicStroke(2.5f));
            g2.draw(inner);
            g2.setStroke(new BasicStroke(1));
        }

        // 제목줄
        g2.setFont(Theme.font(Font.BOLD, 16));
        g2.setColor(t.accentLight);
        g2.drawString("◆ 현재 목표", PAD, 24);
        g2.setFont(Theme.font(Font.BOLD, 14));
        g2.setColor(t.textDim);
        String sign = collapsed ? "펼치기 +" : "접기 −";
        g2.drawString(sign, w - PAD - g2.getFontMetrics().stringWidth(sign), 24);
        if (!collapsed) {
            g2.setColor(new Color(t.accent.getRed(), t.accent.getGreen(), t.accent.getBlue(), 120));
            g2.drawLine(PAD, HEADER + 1, w - PAD, HEADER + 1);
            g2.setFont(Theme.font(Font.PLAIN, 16));
            g2.setColor(t.text);
            int y = HEADER + PAD + 6;
            for (String line : wrap()) {
                g2.drawString(line, PAD + 2, y);
                y += LINE;
            }
        }
        t.pin(g2, 6, 6, 2);
        t.pin(g2, w - 8, 6, 2);
        g2.dispose();
    }
}
