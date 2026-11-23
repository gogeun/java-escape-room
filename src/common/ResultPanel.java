package common;

import javax.swing.*;
import java.awt.*;

/**
 * 결과 화면 (탈출 성공 / 게임 오버). 점수, 등급, 플레이 통계를 보여준다.
 */
public class ResultPanel extends JPanel {
    private GameResult result;

    public ResultPanel(MainFrame frame) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_DARK);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);
        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 60, 0));

        JButton retry = Theme.button("다시 도전", 18);
        retry.addActionListener(e -> GameManager.getInstance().startNewGame(
            result.getPlayerName(), result.getDifficulty()));
        JButton rank = Theme.button("명예의 전당", 18);
        rank.addActionListener(e -> new RankingDialog(frame).setVisible(true));
        JButton title = Theme.button("타이틀로", 18);
        title.addActionListener(e -> frame.showTitle());

        buttons.add(retry);
        buttons.add(rank);
        buttons.add(title);
        add(buttons, BorderLayout.SOUTH);
    }

    public void setResult(GameResult r) {
        this.result = r;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (result == null) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        boolean ok = result.isCleared();

        Color top = ok ? new Color(55, 42, 18) : new Color(60, 12, 12);
        g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), Theme.BG_DARK));
        g2.fillRect(0, 0, w, getHeight());

        centered(g2, ok ? "탈출 성공!" : "GAME OVER", 120, Theme.font(Font.BOLD, 64),
            ok ? Theme.GOLD_LIGHT : Theme.DANGER);
        centered(g2, result.getReason(), 172, Theme.font(Font.PLAIN, 20), Theme.TEXT);

        // 등급 원
        int cx = w / 2, cy = 290, r = 72;
        g2.setColor(new Color(0, 0, 0, 110));
        g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2.setColor(ok ? Theme.GOLD : Theme.DANGER);
        g2.setStroke(new BasicStroke(4f));
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        centered(g2, result.getGrade(), cy + 30, Theme.font(Font.BOLD, 86), ok ? Theme.GOLD_LIGHT : Theme.DANGER);

        centered(g2, "SCORE  " + result.getScore(), 410, Theme.font(Font.BOLD, 34), Theme.GOLD_LIGHT);

        String[][] stats = {
            {"플레이어", result.getPlayerName() + "  (" + result.getDifficulty().getLabel() + ")"},
            {"플레이 시간", GameTimer.format(result.getElapsedSec())},
            {"남은 체력", result.getHp() + " / " + result.getMaxHp()},
            {"탈출한 방", result.getRoomsCleared() + " 개"},
            {"해결한 퍼즐", result.getPuzzlesSolved() + " 개"},
            {"사용한 힌트", result.getHintsUsed() + " 개"},
            {"오답 횟수", result.getWrongCount() + " 회"},
        };
        g2.setFont(Theme.font(Font.PLAIN, 18));
        int y = 465;
        int colW = 300;
        for (int i = 0; i < stats.length; i++) {
            int col = i % 2;
            int row = i / 2;
            int x = w / 2 - colW + col * colW + 20;
            int yy = y + row * 32;
            g2.setColor(Theme.TEXT_DIM);
            g2.drawString(stats[i][0], x, yy);
            g2.setColor(Theme.TEXT);
            g2.drawString(stats[i][1], x + 120, yy);
        }
        g2.dispose();
    }

    private void centered(Graphics2D g2, String s, int y, Font f, Color c) {
        g2.setFont(f);
        g2.setColor(c);
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, (getWidth() - sw) / 2, y);
    }
}
