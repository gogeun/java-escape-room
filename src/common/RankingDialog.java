package common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 명예의 전당 (탈출 성공 기록 TOP 10). 난이도별 필터 가능.
 */
public class RankingDialog extends DialogBox {
    private final DefaultTableModel model;

    public RankingDialog(Window owner) {
        super(owner, "명예의 전당", 640, 480);

        String[] cols = {"순위", "이름", "난이도", "점수", "등급", "클리어 시간", "날짜"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(Theme.font(Font.PLAIN, 14));
        table.setBackground(Theme.BG_PANEL);
        table.setForeground(Theme.TEXT);
        table.setGridColor(new Color(90, 70, 45));
        table.setSelectionBackground(Theme.BG_BUTTON_HOVER);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 14));
        table.getTableHeader().setBackground(Theme.BG_BUTTON);
        table.getTableHeader().setForeground(Theme.GOLD_LIGHT);
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < cols.length; i++) table.getColumnModel().getColumn(i).setCellRenderer(center);

        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(Theme.BG_PANEL);
        sp.setBorder(Theme.goldBorder(1));

        JComboBox<Object> filter = new JComboBox<>(new Object[]{"전체", Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD});
        filter.setFont(Theme.font(Font.PLAIN, 14));
        filter.addActionListener(e -> load(filter.getSelectedItem()));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        top.setOpaque(false);
        top.add(Theme.label("난이도  ", Font.PLAIN, 14, Theme.TEXT));
        top.add(filter);

        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);
        setContent(p);

        addButton("기록 초기화", () -> {
            if (DialogBox.confirm(this, "기록 초기화", "모든 랭킹 기록을 삭제할까요?")) {
                RankingManager.clear();
                load(filter.getSelectedItem());
            }
        });
        addButton("닫기", this::dispose);
        load("전체");
    }

    private void load(Object filter) {
        model.setRowCount(0);
        List<RankingManager.Entry> list = (filter instanceof Difficulty)
            ? RankingManager.search(((Difficulty) filter).name())   // 난이도로 검색
            : RankingManager.loadSorted();
        int rank = 1;
        for (RankingManager.Entry e : list) {
            if (filter instanceof Difficulty && e.difficulty != filter) continue;
            model.addRow(new Object[]{rank, e.name, e.difficulty.getLabel(), e.score, e.grade,
                GameTimer.format(e.clearSec), e.date});
            if (++rank > RankingManager.MAX_SHOW) break;
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[]{"-", "기록 없음", "", "", "", "", ""});
        }
    }
}
