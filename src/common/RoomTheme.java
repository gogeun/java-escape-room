package common;

import java.awt.*;
import java.awt.geom.Path2D;

/**
 * 방마다 다른 UI 테마 (상단 바 / 목표 상자 / 가방 / 팝업 색).
 * 1번 방의 황동 장치 느낌 UI 를 기본 모양으로 삼고, 재질과 색만 방 분위기에 맞게 바꾼다.
 *
 *  - BRASS     : 1번 방 공중도시의 시간 기관실 (황동 + 비취색 법랑)
 *  - ROYAL     : 2번 방 왕실 계승의 방 (금박 + 진홍 벨벳)
 *  - BLOODMOON : 3번 방 혈월의 예배당 (검은 철 + 핏빛)
 *  - DEFAULT   : 타이틀 / 결과 화면
 *
 * 각 방은 RoomPanel.getTheme() 을 오버라이드해서 자기 테마를 고른다.
 */
public enum RoomTheme {

    BRASS(
        new Color[]{new Color(255, 223, 164), new Color(169, 118, 62), new Color(204, 161, 97),
            new Color(111, 76, 43), new Color(183, 135, 73), new Color(103, 69, 43), new Color(70, 48, 33)},
        new Color(52, 78, 70), new Color(19, 35, 35),
        new Color(223, 185, 116), new Color(247, 213, 148), new Color(245, 230, 200), new Color(174, 214, 189),
        new Color(232, 96, 78),
        new Color(16, 27, 27), new Color(29, 45, 43), new Color(53, 88, 81), new Color(78, 120, 108)),

    ROYAL(
        new Color[]{new Color(255, 238, 176), new Color(176, 128, 48), new Color(226, 184, 96),
            new Color(122, 84, 30), new Color(198, 152, 64), new Color(110, 74, 28), new Color(70, 46, 18)},
        new Color(96, 28, 38), new Color(38, 10, 16),
        new Color(226, 184, 96), new Color(255, 232, 168), new Color(246, 230, 204), new Color(214, 170, 150),
        new Color(235, 70, 70),
        new Color(30, 12, 16), new Color(52, 22, 28), new Color(98, 34, 42), new Color(132, 50, 58)),

    BLOODMOON(
        new Color[]{new Color(222, 212, 220), new Color(98, 84, 96), new Color(154, 140, 152),
            new Color(58, 46, 58), new Color(124, 108, 122), new Color(52, 38, 50), new Color(26, 18, 26)},
        new Color(64, 14, 24), new Color(14, 5, 11),
        new Color(214, 160, 96), new Color(244, 196, 128), new Color(239, 221, 189), new Color(190, 128, 128),
        new Color(220, 30, 45),
        new Color(20, 11, 16), new Color(40, 20, 28), new Color(86, 22, 32), new Color(124, 34, 48)),

    DEFAULT(
        new Color[]{new Color(255, 228, 168), new Color(160, 112, 58), new Color(205, 165, 85),
            new Color(105, 70, 38), new Color(180, 132, 70), new Color(96, 64, 36), new Color(60, 42, 26)},
        new Color(48, 34, 22), new Color(22, 17, 13),
        new Color(205, 165, 85), new Color(255, 230, 160), new Color(240, 225, 195), new Color(170, 150, 120),
        new Color(220, 60, 50),
        new Color(22, 17, 13), new Color(38, 29, 22), new Color(70, 45, 25), new Color(105, 68, 35));

    private static final float[] METAL_STOPS = {0, .09f, .26f, .53f, .69f, .86f, 1};

    private final Color[] metal;
    public final Color enamelTop, enamelBottom;
    public final Color accent, accentLight, text, textDim, heart;
    /** 팝업(DialogBox 등)에 쓰이는 Theme 색 */
    public final Color bgDark, bgPanel, bgButton, bgButtonHover;

    RoomTheme(Color[] metal, Color enamelTop, Color enamelBottom,
              Color accent, Color accentLight, Color text, Color textDim, Color heart,
              Color bgDark, Color bgPanel, Color bgButton, Color bgButtonHover) {
        this.metal = metal;
        this.enamelTop = enamelTop;
        this.enamelBottom = enamelBottom;
        this.accent = accent;
        this.accentLight = accentLight;
        this.text = text;
        this.textDim = textDim;
        this.heart = heart;
        this.bgDark = bgDark;
        this.bgPanel = bgPanel;
        this.bgButton = bgButton;
        this.bgButtonHover = bgButtonHover;
    }

    // ------------------------------------------------------------------
    // 그리기 도우미 (1번 방 HudMetal 과 같은 방식, 색만 테마별)
    // ------------------------------------------------------------------

    /** 금속 테두리 그라데이션 */
    public LinearGradientPaint metal(float y, float height) {
        return new LinearGradientPaint(0, y, 0, y + Math.max(height, 1), METAL_STOPS, metal);
    }

    /** 모서리를 깎은 팔각 판 모양 */
    public static Shape plate(int x, int y, int w, int h, int cut) {
        Path2D p = new Path2D.Double();
        p.moveTo(x + cut, y);
        p.lineTo(x + w - cut, y);
        p.lineTo(x + w, y + cut);
        p.lineTo(x + w, y + h - cut);
        p.lineTo(x + w - cut, y + h);
        p.lineTo(x + cut, y + h);
        p.lineTo(x, y + h - cut);
        p.lineTo(x, y + cut);
        p.closePath();
        return p;
    }

    /** 금속 몸체 (그림자 + 금속 그라데이션 + 외곽선) */
    public void chassis(Graphics2D g, Shape shape) {
        Rectangle b = shape.getBounds();
        g.translate(0, 2);
        g.setColor(new Color(8, 8, 10, 150));
        g.setStroke(new BasicStroke(3));
        g.draw(shape);
        g.translate(0, -2);
        g.setPaint(metal(b.y, b.height));
        g.fill(shape);
        g.setStroke(new BasicStroke(1));
        g.setColor(metal[6].darker());
        g.draw(shape);
    }

    /** 안쪽 법랑(색 판) */
    public void enamel(Graphics2D g, Shape shape) {
        Rectangle b = shape.getBounds();
        g.setPaint(new GradientPaint(0, b.y, enamelTop, 0, b.y + b.height, enamelBottom));
        g.fill(shape);
        g.setColor(new Color(0, 0, 0, 110));
        g.draw(shape);
    }

    /** 작은 리벳 */
    public void pin(Graphics2D g, int x, int y, int r) {
        g.setColor(new Color(10, 10, 10, 160));
        g.fillOval(x - r - 1, y - r, r * 2 + 2, r * 2 + 2);
        g.setPaint(metal(y - r, r * 2));
        g.fillOval(x - r, y - r, r * 2, r * 2);
        g.setColor(metal[5]);
        g.drawLine(x - r / 2, y + r / 2, x + r / 2, y - r / 2);
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    /** 현재 방 테마 (방이 없으면 DEFAULT) */
    public static RoomTheme current() {
        GameManager gm = GameManager.getInstance();
        RoomPanel room = gm.isRunning() ? gm.getCurrentRoom() : null;
        return room == null ? DEFAULT : room.getTheme();
    }
}
