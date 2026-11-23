package common;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 획득한 아이템을 슬롯 형태로 보여주는 인벤토리.
 * 슬롯을 클릭하면 아이템 확대보기(Item.view())가 열리고, 마우스를 올리면 설명이 뜬다.
 */
public class InventoryPanel extends JPanel implements GameListener {
    public static final int SLOT_COUNT = 8;
    private final JPanel grid;
    private final JLabel countLabel;

    public InventoryPanel() {
        setLayout(new BorderLayout(0, 4));
        setOpaque(false);

        countLabel = Theme.label("", Font.BOLD, 14, Theme.GOLD_LIGHT);
        add(countLabel, BorderLayout.NORTH);

        grid = new JPanel(new GridLayout(0, 2, 6, 6));
        grid.setOpaque(false);
        add(grid, BorderLayout.CENTER);

        GameManager.getInstance().addListener(this);
        refresh();
    }

    @Override
    public void onInventoryChanged() {
        refresh();
    }

    private void refresh() {
        grid.removeAll();
        List<Item> items = GameManager.getInstance().getInventory();
        countLabel.setText("가방  (" + items.size() + ")");
        for (int i = 0; i < Math.max(SLOT_COUNT, items.size()); i++) {
            grid.add(i < items.size() ? createSlot(items.get(i)) : createEmptySlot());
        }
        grid.revalidate();
        grid.repaint();
    }

    private JComponent createEmptySlot() {
        JPanel p = new JPanel();
        p.setBackground(new Color(30, 23, 17));
        p.setBorder(BorderFactory.createLineBorder(new Color(80, 62, 40)));
        p.setPreferredSize(new Dimension(84, 62));
        return p;
    }

    private JComponent createSlot(Item item) {
        JButton b = new JButton();
        b.setLayout(new BorderLayout());
        b.setBackground(new Color(55, 40, 26));
        b.setBorder(Theme.goldBorder(1));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(84, 62));
        b.setToolTipText("<html><b>" + item.getName() + "</b><br>" + item.getDescription()
            + (item.isViewable() ? "<br><i>(클릭하여 자세히 보기)</i>" : "") + "</html>");

        if (item.getIcon() != null) {
            Image scaled = item.getIcon().getScaledInstance(48, 36, Image.SCALE_SMOOTH);
            JLabel icon = new JLabel(new ImageIcon(scaled));
            b.add(icon, BorderLayout.CENTER);
        }
        JLabel name = Theme.label(item.getName(), Font.BOLD, 11, Theme.TEXT);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        b.add(name, item.getIcon() != null ? BorderLayout.SOUTH : BorderLayout.CENTER);

        b.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Effect.CLICK);
            item.view();
        });
        return b;
    }
}
