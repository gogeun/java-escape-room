package common;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 큰 배경 그림을 빠르게 그리는 도우미.
 *
 * 화면 배율(예: 1.4배)이 적용된 상태에서 큰 그림을 매번 확대해 그리면 한 번에 10ms 이상 걸려
 * 애니메이션이 있는 방(1초에 30번 다시 그림)에서 버벅임이 생긴다.
 * 같은 크기로 계속 그리는 그림은 실제 화면 해상도로 한 번만 확대해 두고, 그다음부터는 확대 없이 그대로 찍는다.
 * 크기가 계속 바뀌는 중(흔들림 · 확대 연출 등)에는 평소처럼 바로 그린다.
 */
public final class FastImage {
    private FastImage() {}

    private static final int STABLE_FRAMES = 3;   // 같은 크기로 이만큼 그리면 확대본을 만든다
    private static final Map<Image, Entry> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private static final class Entry {
        Image scaled;
        int width, height;
        int pendingW, pendingH, seen;
    }

    /** g 의 현재 좌표계 기준 (x, y, w, h) 영역에 img 를 그린다 */
    public static void draw(Graphics2D g, Image img, double x, double y, double w, double h) {
        if (img == null) return;
        AffineTransform t = g.getTransform();
        if (t.getShearX() != 0 || t.getShearY() != 0) {           // 회전된 좌표계는 그대로 그림
            drawDirect(g, img, x, y, w, h);
            return;
        }
        int pw = (int) Math.round(w * t.getScaleX()), ph = (int) Math.round(h * t.getScaleY());
        if (pw <= 0 || ph <= 0) return;

        Entry e = CACHE.computeIfAbsent(img, k -> new Entry());
        if (e.scaled == null || e.width != pw || e.height != ph) {
            if (e.pendingW == pw && e.pendingH == ph) e.seen++;
            else { e.pendingW = pw; e.pendingH = ph; e.seen = 1; }
            if (e.seen < STABLE_FRAMES) {                            // 크기가 아직 바뀌는 중
                drawDirect(g, img, x, y, w, h);
                return;
            }
            e.scaled = buildScaled(g, img, pw, ph);
            e.width = pw;
            e.height = ph;
        }
        double dx = t.getTranslateX() + x * t.getScaleX(), dy = t.getTranslateY() + y * t.getScaleY();
        g.setTransform(new AffineTransform());                       // 화면 픽셀 그대로 찍기
        g.drawImage(e.scaled, (int) Math.round(dx), (int) Math.round(dy), null);
        g.setTransform(t);
    }

    private static void drawDirect(Graphics2D g, Image img, double x, double y, double w, double h) {
        g.drawImage(img, (int) Math.round(x), (int) Math.round(y), (int) Math.round(w), (int) Math.round(h), null);
    }

    /** 화면 장치와 같은 형식(가속 가능한 형식)으로 한 번 고품질 확대 */
    private static Image buildScaled(Graphics2D g, Image img, int pw, int ph) {
        boolean alpha = !(img instanceof BufferedImage b) || b.getColorModel().hasAlpha();
        GraphicsConfiguration gc = g.getDeviceConfiguration();
        BufferedImage out = gc != null
            ? gc.createCompatibleImage(pw, ph, alpha ? Transparency.TRANSLUCENT : Transparency.OPAQUE)
            : new BufferedImage(pw, ph, alpha ? BufferedImage.TYPE_INT_ARGB_PRE : BufferedImage.TYPE_INT_RGB);
        Graphics2D s = out.createGraphics();
        s.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        s.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        s.drawImage(img, 0, 0, pw, ph, null);
        s.dispose();
        return out;
    }
}
