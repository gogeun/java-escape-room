package common;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * 방 화면 아래 가운데에 떠 있는 가방 (1번 방의 공구 거치대 모양 인벤토리).
 *  - 칸에 마우스를 올리면 아이템 이름이 위에 뜬다
 *  - 클릭하면 아이템 확대보기(Item.view())가 열리고, 그 칸이 선택 표시된다
 *  - 새 아이템이 들어오면 칸이 반짝인다
 *  - 색과 재질은 현재 방의 RoomTheme 을 따른다
 */
public class InventoryDock extends JComponent implements GameListener {
    private static final int MIN_SLOTS = 8, CELL = 58, GAP = 4, WING = 28, PAD_Y = 8, LABEL = 30;
    private static final int PLATE_H = CELL + PAD_Y * 2 + 5;

    private List<Item> items = List.of();
    private int hover = -1;
    private String selectedId;
    private String newestId;
    private long pickedUp;
    private final Timer glint = new Timer(1000 / 30, e -> {
        if (System.currentTimeMillis() - pickedUp > 700) ((Timer) e.getSource()).stop();
        repaint();
    });

    public InventoryDock() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int i = slotAt(e.getX(), e.getY());
                if (i != hover) {
                    hover = i;
                    setCursor(i >= 0 && i < items.size() ? new Cursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
                    repaint();
                }
            }
            @Override public void mouseExited(MouseEvent e) { hover = -1; repaint(); }
            @Override public void mouseClicked(MouseEvent e) {
                int i = slotAt(e.getX(), e.getY());
                if (i < 0 || i >= items.size()) return;
                Item item = items.get(i);
                selectedId = item.getId().equals(selectedId) ? null : item.getId();
                SoundManager.getInstance().play(SoundManager.Effect.CLICK);
                repaint();
                item.view();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        GameManager.getInstance().addListener(this);
        refresh();
    }

    @Override public void onInventoryChanged() { refresh(); }
    @Override public void onRoomChanged(RoomPanel room, int index, int count) { selectedId = null; refresh(); }

    private void refresh() {
        List<Item> now = GameManager.getInstance().getInventory();
        if (now.size() > items.size()) {               // 새 아이템 → 반짝임
            newestId = now.get(now.size() - 1).getId();
            pickedUp = System.currentTimeMillis();
            glint.restart();
        }
        items = List.copyOf(now);
        if (selectedId != null && items.stream().noneMatch(i -> i.getId().equals(selectedId))) selectedId = null;
        int slots = slotCount();
        int w = slots * CELL + (slots - 1) * GAP + WING * 2;
        int h = PLATE_H + LABEL;
        setBounds((RoomPanel.ROOM_WIDTH - w) / 2, RoomPanel.ROOM_HEIGHT - h - 6, w, h);
        repaint();
    }

    private int slotCount() { return Math.max(MIN_SLOTS, items.size()); }

    private int slotX(int i) { return WING + i * (CELL + GAP); }

    private int slotAt(int x, int y) {
        int top = LABEL + PAD_Y;
        if (y < top || y > top + CELL) return -1;
        for (int i = 0; i < slotCount(); i++) {
            if (x >= slotX(i) && x < slotX(i) + CELL) return i;
        }
        return -1;
    }

    /** 가방 판 위에서만 마우스를 받는다 (이름표 자리는 방 화면으로 통과) */
    @Override public boolean contains(int x, int y) {
        return x >= 0 && x < getWidth() && y >= LABEL && y < getHeight();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        RoomTheme.smooth(g2);
        RoomTheme t = RoomTheme.current();
        int w = getWidth(), slots = slotCount();

        // ---- 거치대 몸체 ----
        int py = LABEL;
        t.chassis(g2, RoomTheme.plate(1, py + 1, w - 3, PLATE_H - 4, 10));
        t.enamel(g2, RoomTheme.plate(WING - 9, py + 5, w - (WING - 9) * 2 - 1, PLATE_H - 12, 6));
        t.pin(g2, 11, py + PLATE_H / 2 - 1, 3);
        t.pin(g2, w - 12, py + PLATE_H / 2 - 1, 3);
        g2.setFont(Theme.font(Font.BOLD, 10));
        g2.setColor(t.textDim);

        // ---- 칸 ----
        int sy = py + PAD_Y;
        for (int i = 0; i < slots; i++) {
            int sx = slotX(i);
            g2.setColor(new Color(0, 0, 0, 120));
            g2.fillRect(sx, sy, CELL, CELL);
            g2.setColor(new Color(0, 0, 0, 170));
            g2.drawRect(sx, sy, CELL - 1, CELL - 1);
            g2.setColor(new Color(255, 255, 255, 25));
            g2.drawLine(sx + 1, sy + CELL - 1, sx + CELL - 1, sy + CELL - 1);
            if (i >= items.size()) continue;

            Item item = items.get(i);
            boolean selected = item.getId().equals(selectedId);
            if (i == hover || selected) {
                g2.setPaint(new GradientPaint(sx, sy, withAlpha(t.accent, selected ? 120 : 80),
                    sx + CELL, sy + CELL, withAlpha(t.accentLight, 25)));
                g2.fillRect(sx + 1, sy + 1, CELL - 2, CELL - 2);
                g2.setColor(selected ? t.accentLight : t.accent);
                g2.setStroke(new BasicStroke(selected ? 2f : 1f));
                g2.drawRect(sx + 1, sy + 1, CELL - 3, CELL - 3);
                g2.setStroke(new BasicStroke(1));
            }
            drawIcon(g2, item, sx + 5, sy + 5, CELL - 10, t);

            if (item.getId().equals(newestId) && glint.isRunning()) {
                float k = Math.min(1f, (System.currentTimeMillis() - pickedUp) / 700f);
                g2.setColor(withAlpha(t.accentLight, (int) (170 * (1 - k))));
                g2.setStroke(new BasicStroke(2f));
                g2.drawRect(sx + 1, sy + 1, CELL - 3, CELL - 3);
                g2.setStroke(new BasicStroke(1));
            }
        }

        // ---- 이름표 (마우스 올린 칸) ----
        if (hover >= 0 && hover < items.size()) {
            String name = items.get(hover).getName();
            g2.setFont(Theme.font(Font.BOLD, 15));
            FontMetrics fm = g2.getFontMetrics();
            int lw = fm.stringWidth(name) + 20;
            int lx = Math.max(2, Math.min(w - lw - 2, slotX(hover) + CELL / 2 - lw / 2));
            Shape tag = RoomTheme.plate(lx, 1, lw, LABEL - 4, 4);
            g2.setColor(withAlpha(t.bgDark, 235));
            g2.fill(tag);
            g2.setColor(t.accent);
            g2.draw(tag);
            g2.setColor(t.accentLight);
            g2.drawString(name, lx + 10, 20);
        }
        g2.dispose();
    }

    /** 아이콘 그림이 있으면 비율을 지켜 그리고, 없으면 이름 첫 글자를 새긴다 */
    private void drawIcon(Graphics2D g2, Item item, int x, int y, int size, RoomTheme t) {
        Image icon = item.getIcon();
        if (icon != null && icon.getWidth(null) > 0) {
            int iw = icon.getWidth(null), ih = icon.getHeight(null);
            double s = Math.min((double) size / iw, (double) size / ih);
            int dw = (int) (iw * s), dh = (int) (ih * s);
            g2.drawImage(icon, x + (size - dw) / 2, y + (size - dh) / 2, dw, dh, null);
        } else {
            g2.setFont(Theme.font(Font.BOLD, 20));
            String c = item.getName().isEmpty() ? "?" : item.getName().substring(0, 1);
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(t.accentLight);
            g2.drawString(c, x + (size - fm.stringWidth(c)) / 2, y + (size + fm.getAscent() - fm.getDescent()) / 2);
        }
    }

    private static Color withAlpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}
