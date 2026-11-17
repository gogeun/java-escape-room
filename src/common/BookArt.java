package common;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Drawings share one design coordinate system with the name field and keyboard-accessible hotspots. */
final class BookArt {
    static final int WIDTH = 1200, HEIGHT = 700;
    static final Rectangle SHELF_HIT = new Rectangle(762, 207, 236, 388);
    static final Rectangle SIGN_HIT = new Rectangle(340, 456, 155, 59);
    static final Rectangle NAME_FIELD = new Rectangle(295, 367, 245, 49);
    static final Color INK = new Color(57, 42, 28);
    private static final Color IVORY = new Color(248, 235, 202);
    private static final Color GOLD = new Color(216, 175, 104);
    private static final Color COPPER = new Color(132, 92, 52);
    private static final Color HAIRLINE = new Color(139, 111, 72);
    private static final int SPINE = 600, TOP = 120, PAGE_W = 365, PAGE_H = 450;
    private static final int PRINT_X = 28, PRINT_Y = 147, PRINT_W = 308;
    private static final double PRINT_H = PRINT_W * 720.0 / 1280;
    private static final String SERIF = chooseFamily(new String[] { "Batang", "Noto Serif CJK KR", Font.SERIF });
    private static final String SANS = chooseFamily(new String[] { "Malgun Gothic", "Noto Sans CJK KR", Font.DIALOG });
    private static final TexturePaint FIBERS = paperFibers();

    private final BufferedImage libraryPlate;
    private final BufferedImage shelfPlate;
    private final BufferedImage coverPlate;
    private final BufferedImage leftPlate;
    private final BufferedImage rightPlate;
    private final BufferedImage readingPlate;
    private final BufferedImage workshop;
    private final BufferedImage etching;

    BookArt(BufferedImage library, BufferedImage coverImage, BufferedImage roomImage) {
        libraryPlate = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(libraryPlate);
        library(g, library);
        g.dispose();

        shelfPlate = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        g = pen(shelfPlate);
        g.drawImage(libraryPlate,0,0,null);
        shelfFittings(g);
        g.dispose();
        g = pen(libraryPlate);
        dim(g,.72);
        g.dispose();

        coverPlate = page();
        g = pen(coverPlate);
        cover(g, coverImage);
        g.dispose();
        leftPlate = page();
        g = pen(leftPlate);
        leftPage(g);
        g.dispose();
        rightPlate = page();
        g = pen(rightPlate);
        rightPage(g);
        g.dispose();
        workshop = roomImage;
        etching = makeEtching(roomImage);
        g=pen(rightPlate);
        drawEtching(g);
        g.dispose();
        readingPlate = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        g=pen(readingPlate);
        g.drawImage(libraryPlate,0,0,null);
        g.translate(SPINE,TOP);
        binding(g,Math.PI);
        g.drawImage(leftPlate,-PAGE_W,0,null);
        g.drawImage(rightPlate,0,0,null);
        g.dispose();
    }

    private static BufferedImage page() {
        return new BufferedImage(PAGE_W, PAGE_H, BufferedImage.TYPE_INT_ARGB_PRE);
    }

    private static Graphics2D pen(BufferedImage target) {
        Graphics2D g = target.createGraphics();
        quality(g);
        return g;
    }

    private static BufferedImage makeEtching(BufferedImage original) {
        int w = PRINT_W * 2, h = (int) Math.round(PRINT_H * 2);
        BufferedImage plate = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = pen(plate);
        g.drawImage(original, 0, 0, w, h, null);
        g.dispose();
        int[] rgb = plate.getRGB(0,0,w,h,null,0,w);
        int[] gray = new int[rgb.length];
        for (int i=0; i<rgb.length; i++) {
            int c = rgb[i];
            gray[i] = (54*((c>>16)&255) + 183*((c>>8)&255) + 19*(c&255)) >> 8;
        }
        for (int y=0; y<h; y++) for (int x=0; x<w; x++) {
            int p = y*w+x, v = gray[p];
            int line = Math.abs(v - gray[y*w+Math.max(0,x-1)]) + Math.abs(v-gray[Math.max(0,y-1)*w+x]);
            double tone = Math.max(0, Math.min(1, (.18+.82*Math.sqrt(v/255.0)) - Math.min(.34, line/180.0)));
            if ((x+2*y)%7==0 && v<134) tone *= .88;
            int r=(int)(61+174*tone), green=(int)(53+159*tone), b=(int)(39+132*tone);
            rgb[p] = 0xff000000 | (r<<16) | (green<<8) | b;
        }
        plate.setRGB(0,0,w,h,rgb,0,w);
        return plate;
    }

    private static String chooseFamily(String[] candidates) {
        for (String candidate : candidates) {
            Font f = new Font(candidate, Font.PLAIN, 12);
            if (f.getFamily().equalsIgnoreCase(candidate) && f.canDisplay('서')) return candidate;
        }
        return Font.DIALOG;
    }

    static Font serif(int style, int size) { return new Font(SERIF, style, size); }
    private static Font sans(int style, int size) { return new Font(SANS, style, size); }

    static void quality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    static void library(Graphics2D g, BufferedImage art) {
        if (art != null) {
            double scale = Math.max(WIDTH / (double) art.getWidth(), HEIGHT / (double) art.getHeight());
            int w = (int) Math.ceil(scale * art.getWidth()), h = (int) Math.ceil(scale * art.getHeight());
            g.drawImage(art, (WIDTH - w) / 2, (HEIGHT - h) / 2, w, h, null);
        } else {
            g.setPaint(new GradientPaint(80, 0, new Color(53, 74, 70), 1120, 690, new Color(30, 23, 20)));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            for (int i = 0; i < 3; i++) {
                int bottom = 183 + i * 186;
                for (int j = 0; j < 33; j++) {
                    int height = 94 + (j * 19 + i * 7) % 60;
                    boundSpine(g, j * 37, bottom - height, 35, height, j + i);
                }
                wood(g, 0, bottom, WIDTH, 17);
            }
        }
        g.setPaint(new GradientPaint(5, 310, new Color(7, 16, 20, 198), 700, 310, new Color(7, 16, 20, 0)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        g.setPaint(new GradientPaint(0, 540, new Color(8, 5, 4, 0), 0, 700, new Color(8, 5, 4, 150)));
        g.fillRect(0, 540, WIDTH, 160);
    }

    private static void write(Graphics2D g, String text, Font font, Color color, int x, int y, boolean centered) {
        g.setColor(color);
        g.setFont(font);
        g.drawString(text, centered ? x - g.getFontMetrics().stringWidth(text) / 2 : x, y);
    }

    private static void diamond(Graphics2D g, int x, int y, int r) {
        g.fillPolygon(new int[] { x, x+r, x, x-r }, new int[] { y-r, y, y+r, y }, 4);
    }

    private static void divider(Graphics2D g, int x, int y, int halfWidth, Color color) {
        g.setColor(color);
        g.drawLine(x-halfWidth, y, x-8, y);
        g.drawLine(x+8, y, x+halfWidth, y);
        diamond(g, x, y, 4);
    }

    private static void wood(Graphics2D g, int x, int y, int w, int h) {
        g.setPaint(new GradientPaint(x, y, new Color(142, 95, 55), x, y+h, new Color(48, 31, 23)));
        g.fillRoundRect(x, y, w, h, 4, 4);
        g.setColor(new Color(224, 172, 102, 140));
        g.drawLine(x+2, y+2, x+w-2, y+2);
        g.setColor(new Color(30, 18, 13, 150));
        g.drawLine(x+1, y+h-3, x+w-1, y+h-3);
    }

    private static final Color[] BINDINGS = {
        new Color(80, 52, 42), new Color(54, 67, 58), new Color(97, 63, 43), new Color(64, 63, 69)
    };

    private static void boundSpine(Graphics2D g, int x, int y, int w, int h, int ink) {
        Color mid = BINDINGS[ink % BINDINGS.length];
        g.setPaint(new GradientPaint(x, y, mid.brighter(), x+w, y, mid.darker()));
        g.fillRoundRect(x, y, w, h, 5, 5);
        g.setColor(new Color(192, 156, 105, 165));
        for (int offset : new int[] { 12, 17, h-23, h-18 })
            g.drawLine(x+3, y+offset, x+w-3, y+offset);
        g.drawRect(x+4, y+h/3, w-9, h/3);
    }

    static void dim(Graphics2D g, double alpha) {
        g.setColor(new Color(12, 14, 15, (int) (255 * Math.max(0, Math.min(1, alpha)))));
        g.fillRect(-WIDTH, -HEIGHT, WIDTH*3, HEIGHT*3);
    }

    private static void shelfFittings(Graphics2D shelf) {
        shelf.setColor(new Color(11, 12, 10, 165));
        shelf.fillRoundRect(702, 279, 341, 247, 6, 6);
        wood(shelf, 693, 262, 14, 270);
        wood(shelf, 1041, 262, 14, 270);
        boundSpine(shelf, 710, 345, 23, 179, 1);
        boundSpine(shelf, 735, 319, 29, 205, 2);
        boundSpine(shelf, 991, 346, 25, 178, 0);
        boundSpine(shelf, 1017, 362, 22, 162, 3);
        wood(shelf, 680, 522, 391, 19);
        shelf.setColor(GOLD);
        divider(shelf, 155, 267, 53, GOLD);
        write(shelf, "시간의 서가", serif(Font.BOLD, 68), IVORY, 95, 358, false);
        write(shelf, "서가에서 항해 일지를 꺼내세요.", sans(Font.PLAIN, 19), IVORY, 101, 413, false);
        write(shelf, "책 꺼내기", sans(Font.BOLD, 18), IVORY, 879, 577, true);
        divider(shelf, 879, 590, 45, GOLD);
    }

    private static void compass(Graphics2D g, int x, int y, int r, Color color) {
        Graphics2D a = (Graphics2D) g.create();
        a.translate(x, y);
        a.setColor(color);
        a.setStroke(new BasicStroke(1.4f));
        a.drawOval(-r,-r,2*r,2*r);
        a.drawOval(-r+5,-r+5,2*r-10,2*r-10);
        for (int i=0; i<32; i++) {
            a.drawLine(0, -r+5, 0, -r+(i%4==0?13:8));
            a.rotate(Math.PI/16);
        }
        int longRay = (int) (.8 * r);
        for (int i=0; i<8; i++) {
            int ray = i%2==0 ? longRay : (int)(longRay*.65);
            int foot = Math.max(2, r/10);
            a.fillPolygon(new int[]{0,0,-foot},new int[]{-ray,0,-foot},3);
            a.drawPolygon(new int[]{0,foot,0},new int[]{-ray,-foot,0},3);
            a.rotate(Math.PI/4);
        }
        a.dispose();
    }

    private static void cover(Graphics2D g, BufferedImage art) {
        int w = PAGE_W, h = PAGE_H;
        g.setStroke(new BasicStroke(1));
        g.setColor(new Color(7, 7, 6, 145));
        g.fillRoundRect(7, 7, w, h, 8, 8);
        if (art != null) {
            g.clip(new RoundRectangle2D.Double(0, 0, w, h, 8, 8));
            g.drawImage(art, 0, 0, w, h, null);
            int center = w/2+8;
            write(g, "항해 일지", serif(Font.BOLD, 36), new Color(46,35,19), center+1, 336, true);
            write(g, "항해 일지", serif(Font.BOLD, 36), IVORY, center, 334, true);
            return;
        }
        g.setPaint(new GradientPaint(0, 0, new Color(63, 101, 96), w, h, new Color(16, 49, 52)));
        g.fillRoundRect(0, 0, w, h, 7, 7);
        g.setColor(new Color(34, 51, 48));
        g.fillRect(4, 0, 20, h);
        g.setColor(GOLD);
        g.drawLine(26, 8, 26, h-8);
        g.drawRect(36, 12, w-49, h-25);
        g.drawRect(41, 17, w-59, h-35);
        for (int y=30; y<h-25; y+=17) {
            g.setColor(new Color(234, 207, 142, 20));
            g.drawLine(44, y, 54, y-10);
            g.drawLine(w-34, y+9, w-23, y-2);
        }
        for (int y : new int[]{35, 42, h-42, h-35}) {
            g.setColor(GOLD);
            g.drawLine(4, y, 23, y);
        }
        int cx = w/2+12;
        divider(g, cx, 94, 68, GOLD);
        compass(g, cx, 189, 61, GOLD);
        write(g, "항해 일지", serif(Font.BOLD, 40), IVORY, cx, 334, true);
        divider(g, cx, 361, 72, GOLD);
    }

    private static double ease(double t) {
        t = Math.max(0, Math.min(1, t));
        return t*t*t*(10+t*(-15+6*t));
    }

    private static double mix(double a, double b, double t) { return a+(b-a)*t; }

    /** The binding moves forward while its cover is already starting to turn. */
    void unfold(Graphics2D g, double progress, boolean highlight) {
        double pull = ease(progress/.86);
        double angle = ease((progress-.23)/.77)*Math.PI;
        drawLibrary(g, pull);
        Graphics2D b=(Graphics2D)g.create();
        b.translate(mix(767, SPINE, pull), mix(212, TOP, pull)-23*Math.sin(Math.PI*pull));
        b.rotate(-.047*Math.sin(Math.PI*pull));
        b.scale(mix(221.0/PAGE_W,1,pull), mix(310.0/PAGE_H,1,pull));
        if (highlight && progress==0) {
            b.setColor(new Color(255,226,166,138));
            b.setStroke(new BasicStroke(3));
            b.drawRoundRect(-4,-4,PAGE_W+7,PAGE_H+7,9,9);
        }
        binding(b,angle);
        if (angle<.015) {
            b.drawImage(coverPlate,0,0,null);
        } else {
            b.drawImage(rightPlate,0,0,null);
            turningLeaf(b,angle);
        }
        b.dispose();
    }

    private void drawLibrary(Graphics2D g, double pull) {
        if (pull < 1) {
            g.drawImage(shelfPlate,0,0,null);
        }
        if (pull > 0) {
            if (pull < 1) g.setComposite(AlphaComposite.SrcOver.derive((float)pull));
            g.drawImage(libraryPlate,0,0,null);
            g.setComposite(AlphaComposite.SrcOver);
        }
    }

    private static void binding(Graphics2D g, double angle) {
        int left=(int)Math.min(0, Math.cos(angle)*PAGE_W);
        int rise=(int)(9*Math.sin(angle));
        g.setColor(new Color(12,11,9,130));
        g.fillRoundRect(left-3,8+rise,PAGE_W-left+13,PAGE_H+5,9,9);
        if (angle<=.015) return;
        g.setColor(new Color(49,64,57));
        g.fillRoundRect(left-4,4,PAGE_W-left+10,PAGE_H+7,9,9);
        g.setColor(new Color(176,146,100));
        g.fillRoundRect(left-1,2,PAGE_W-left+4,PAGE_H+5,7,7);
        g.setColor(new Color(225,202,149));
        g.drawLine(left+3,PAGE_H+3,PAGE_W-1,PAGE_H+3);
    }

    private void turningLeaf(Graphics2D b, double angle) {
        double c=Math.cos(angle), s=Math.sin(angle);
        BufferedImage face=c>=0?coverPlate:leftPlate;
        if (angle>Math.PI-.007) {
            b.drawImage(leftPlate,-PAGE_W,0,null);
            return;
        }
        // Project narrow strips from the same cylindrical hinge, including the lift at the fore edge.
        int n=24, previousX=0;
        for (int i=1; i<=n; i++) {
            double u=i/(double)n, mid=(i-.5)/n;
            int x=(int)Math.round(c*PAGE_W*u/(1-.12*s*u));
            int lift=(int)Math.round(23*s*mid);
            if (previousX!=x) {
                int a=PAGE_W*(i-1)/n, z=PAGE_W*i/n;
                b.drawImage(face,previousX,-lift,x,PAGE_H+lift,
                        c>=0?a:PAGE_W-a,0,c>=0?z:PAGE_W-z,PAGE_H,null);
            }
            previousX=x;
        }
        b.setColor(new Color(62,38,16,(int)(58*s)));
        int tip=(int)Math.round(23*s);
        b.fillPolygon(new int[]{0,previousX,previousX,0},new int[]{0,-tip,PAGE_H+tip,PAGE_H},4);
        b.setColor(new Color(225,201,152,(int)(210*s)));
        b.drawLine(previousX,-tip,previousX,PAGE_H+tip);
    }

    /** Move the printed plate to the actual room's full-bleed bounds without an intervening cut. */
    void register(Graphics2D g, Rectangle2D room, double travel, boolean error, boolean hover, String signature) {
        double camera=ease((travel-.08)/.92);
        double originX=SPINE+PRINT_X, originY=TOP+PRINT_Y;
        double scaleX=Math.pow(room.getWidth()/PRINT_W,camera);
        double scaleY=Math.pow(room.getHeight()/PRINT_H,camera);
        double centerX=mix(originX+PRINT_W/2.0,room.getCenterX(),camera);
        double centerY=mix(originY+PRINT_H/2.0,room.getCenterY(),camera)-12*Math.sin(Math.PI*camera);
        Graphics2D world=(Graphics2D)g.create();
        world.translate(centerX,centerY);
        world.scale(scaleX,scaleY);
        world.translate(-originX-PRINT_W/2.0,-originY-PRINT_H/2.0);
        // At high magnification the illustration covers nearly the entire frame. Skip the
        // center of the cached paper silhouette so there are no redundant full-screen blends.
        double color=ease((travel-.08)/.65);
        if (color<1) {
            world.drawImage(readingPlate,0,0,null);
        } else if (camera<1) {
            Rectangle clip=g.getClipBounds();
            boolean outsideArt=clip!=null && (clip.getMinX()<room.getMinX()-2 || clip.getMaxX()>room.getMaxX()+2
                    || clip.getMinY()<room.getMinY()-2 || clip.getMaxY()>room.getMaxY()+2);
            if (outsideArt) world.setComposite(AlphaComposite.SrcOver.derive((float)(1-ease((camera-.80)/.20))));
            int x=(int)originX, y=(int)originY, right=x+PRINT_W, bottom=(int)(originY+PRINT_H);
            world.drawImage(readingPlate,0,0,WIDTH,y+1,0,0,WIDTH,y+1,null);
            world.drawImage(readingPlate,0,bottom-1,WIDTH,HEIGHT,0,bottom-1,WIDTH,HEIGHT,null);
            world.drawImage(readingPlate,0,y,x+1,bottom,0,y,x+1,bottom,null);
            world.drawImage(readingPlate,right-1,y,WIDTH,bottom,right-1,y,WIDTH,bottom,null);
            world.setComposite(AlphaComposite.SrcOver);
        }
        world.translate(SPINE,TOP);
        if (camera<.995) {
            signature(world,error,hover,signature);
        }
        if (color>0) paintRoom(world,color);
        if (travel>0 && travel<.80) inkTrail(world,travel);
        if (travel>.27 && travel<.9) paperMotes(world,travel);
        world.dispose();
    }

    private void drawEtching(Graphics2D b) {
        AffineTransform plate=AffineTransform.getTranslateInstance(PRINT_X,PRINT_Y);
        plate.scale(PRINT_W/(double)etching.getWidth(),PRINT_H/etching.getHeight());
        b.drawImage(etching,plate,null);
    }

    private void paintRoom(Graphics2D b,double color) {
        AffineTransform mat=AffineTransform.getTranslateInstance(PRINT_X,PRINT_Y);
        mat.scale(PRINT_W/(double)workshop.getWidth(),PRINT_H/workshop.getHeight());
        if (color<1) b.setComposite(AlphaComposite.SrcOver.derive((float)color));
        b.drawImage(workshop,mat,null);
        if (color<1) b.setComposite(AlphaComposite.SrcOver);
    }

    private static void signature(Graphics2D b,boolean error,boolean hover,String value) {
        int fieldX=NAME_FIELD.x-SPINE;
        int lineY=NAME_FIELD.y-TOP+NAME_FIELD.height;
        if (!value.isEmpty()) {
            Graphics2D ink=(Graphics2D)b.create();
            ink.clipRect(fieldX,NAME_FIELD.y-TOP,NAME_FIELD.width,NAME_FIELD.height);
            write(ink,value,serif(Font.BOLD,24),INK,fieldX+NAME_FIELD.width/2,lineY-14,true);
            ink.dispose();
        }
        if (error) {
            b.setColor(new Color(142,53,40));
            b.drawLine(fieldX,lineY,fieldX+NAME_FIELD.width,lineY);
            write(b,"이름을 먼저 적어 주세요.",sans(Font.BOLD,13),new Color(126,50,36),-PAGE_W/2,lineY+24,true);
        }
        if (hover) {
            b.setColor(new Color(113,70,35,90));
            b.drawRoundRect(SIGN_HIT.x-SPINE+3,SIGN_HIT.y-TOP+4,SIGN_HIT.width-6,SIGN_HIT.height-8,10,10);
        }
    }

    private static void inkTrail(Graphics2D b, double travel) {
        // A stroke travels from the signature into the engraving as the camera follows it.
        double head=ease(travel/.56), tail=ease((travel-.19)/.6);
        int alpha=(int)(210*Math.sin(Math.PI*Math.min(1,travel/.8)));
        b.setColor(new Color(142,100,41,alpha));
        b.setStroke(new BasicStroke(1.8f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        for (int i=0; i<35; i++) {
            double t=mix(tail,head,i/35.0), u=mix(tail,head,(i+1)/35.0);
            b.draw(new Line2D.Double(trailX(t),trailY(t),trailX(u),trailY(u)));
        }
        b.setColor(new Color(249,213,134,alpha));
        b.fill(new Ellipse2D.Double(trailX(head)-2.5,trailY(head)-2.5,5,5));
        for (int i=0; i<14; i++) {
            double t=mix(tail,head,i/14.0), spread=7+15*Math.sin(Math.PI*t);
            double x=trailX(t)+spread*Math.sin(11*i+travel*8);
            double y=trailY(t)+spread*Math.cos(17*i+travel*4);
            b.fill(new Ellipse2D.Double(x,y,i%3==0?2.3:1.2,i%3==0?2.3:1.2));
        }
    }

    private static double trailX(double t) { return mix(-168,PRINT_X+PRINT_W*.62,t); }
    private static double trailY(double t) { return mix(295,PRINT_Y+PRINT_H*.48,t)-33*Math.sin(Math.PI*2*t); }

    private static void paperMotes(Graphics2D g,double travel) {
        double t=(travel-.27)/.63;
        int opacity=(int)(100*Math.sin(Math.PI*t));
        g.setColor(new Color(249,222,165,opacity));
        g.setStroke(new BasicStroke(.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i=0;i<14;i++) {
            double angle=i*2.4+.17*Math.sin(t*Math.PI+i);
            double radius=50+7*(i%7)+170*t*t;
            double dx=Math.cos(angle),dy=.65*Math.sin(angle);
            double x=PRINT_X+PRINT_W*.53+radius*dx, y=PRINT_Y+PRINT_H*.47+radius*dy;
            double length=1+5*t*(i%3+1);
            g.draw(new Line2D.Double(x,y,x+dx*length,y+dy*length));
        }
    }

    private static void paper(Graphics2D g, boolean left) {
        int w = PAGE_W, h = PAGE_H;
        Color edge = new Color(199, 168, 118), mid = new Color(242, 222, 176);
        float[] stops = {0, .12f, .7f, 1};
        Color[] colors = left ? new Color[]{edge,mid,new Color(237,214,168), new Color(174,141,97)}
                : new Color[]{new Color(171,138,92),new Color(233,210,162),mid,edge};
        g.setPaint(new LinearGradientPaint(0, 0, w, 0, stops, colors));
        g.fillRoundRect(0, 0, w, h, 6, 6);
        g.setPaint(FIBERS);
        g.fillRoundRect(0, 0, w, h, 6, 6);
        g.setColor(new Color(130, 94, 54, 100));
        g.drawRect(15, 15, w-30, h-31);
        g.setColor(new Color(255, 248, 210, 95));
        g.drawRect(18, 18, w-36, h-37);
        // Engraved corner ornaments, mirrored on all four corners.
        for (int cx : new int[]{25, w-25}) for (int cy : new int[]{25,h-25}) {
            Graphics2D corner = (Graphics2D) g.create();
            corner.translate(cx, cy);
            corner.scale(cx < w/2 ? 1 : -1, cy < h/2 ? 1 : -1);
            corner.setColor(HAIRLINE);
            corner.drawArc(-5,-5,25,25,2,86);
            corner.drawLine(0,0,30,0);
            corner.drawLine(0,0,0,30);
            diamond(corner, 6, 6, 2);
            corner.dispose();
        }
    }

    private static TexturePaint paperFibers() {
        BufferedImage texture = new BufferedImage(137, 109, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = texture.createGraphics();
        Random grain = new Random(1709);
        for (int i=0; i<3000; i++) {
            int x=grain.nextInt(texture.getWidth()), y=grain.nextInt(texture.getHeight());
            g.setColor(i%3==0 ? new Color(75,48,23,18) : new Color(255,249,213,35));
            g.drawLine(x,y,x+grain.nextInt(4),y);
        }
        g.dispose();
        return new TexturePaint(texture, new Rectangle(0,0,texture.getWidth(),texture.getHeight()));
    }

    private static void engraveAirship(Graphics2D g, int x, int y) {
        Graphics2D ship = (Graphics2D) g.create();
        ship.translate(x,y);
        ship.setColor(new Color(122,87,50,170));
        ship.drawOval(-50, -17, 95, 31);
        ship.drawArc(-51, -10, 97, 16, 0, 180);
        ship.drawArc(-49, -4, 93, 16, 185, 167);
        for (int i=-32; i<=30; i+=15) {
            int h = Math.abs(i)/8;
            ship.drawArc(i,-16+h,14,29-2*h,265,192);
        }
        ship.drawPolyline(new int[]{-43,-59,-56,-44},new int[]{-8,-15,-4,0},4);
        ship.drawPolyline(new int[]{-47,-58,-52,-38},new int[]{5,14,16,9},4);
        ship.drawLine(-25,13,-14,25); ship.drawLine(20,12,11,25);
        ship.drawPolygon(new int[]{-18,17,11,-11}, new int[]{24,24,29,29},4);
        ship.drawLine(-88,4,-66,4); ship.drawLine(53,0,83,0);
        ship.dispose();
    }

    private static void leftPage(Graphics2D g) {
        paper(g, true);
        int cx=PAGE_W/2;
        compass(g,cx,82,31,COPPER);
        write(g,"승선 명부",serif(Font.BOLD,39),INK,cx,168,true);
        divider(g,cx,190,65,COPPER);
        write(g,"여행을 시작할 이름을 적어 주세요.",sans(Font.PLAIN,15),INK,cx,225,true);
        int x=NAME_FIELD.x-(SPINE-PAGE_W), y=NAME_FIELD.y-TOP;
        g.setColor(new Color(255,243,207,75));
        g.fillRect(x,y,NAME_FIELD.width,NAME_FIELD.height);
        g.setColor(COPPER);
        g.drawLine(x,y+NAME_FIELD.height,x+NAME_FIELD.width,y+NAME_FIELD.height);
        int sx=SIGN_HIT.x-(SPINE-PAGE_W)+12, sy=SIGN_HIT.y-TOP+9;
        g.setPaint(new GradientPaint(sx,sy,new Color(82,100,82),sx,sy+42,new Color(47,69,61)));
        g.fillRoundRect(sx,sy,SIGN_HIT.width-24,40,8,8);
        g.setColor(new Color(211,181,116));
        g.drawRoundRect(sx+3,sy+3,SIGN_HIT.width-31,33,6,6);
        Path2D feather=new Path2D.Double();
        feather.moveTo(sx+17,sy+27);
        feather.quadTo(sx+21,sy+13,sx+37,sy+8);
        feather.quadTo(sx+37,sy+20,sx+21,sy+25);
        feather.closePath(); g.fill(feather);
        g.setStroke(new BasicStroke(1.3f));
        g.drawLine(sx+15,sy+30,sx+30,sy+16);
        write(g,"서명하기",serif(Font.BOLD,18),IVORY,sx+46,sy+26,false);
    }

    private static void rightPage(Graphics2D g) {
        paper(g,false);
        int cx=PAGE_W/2;
        write(g,"제 1 장",serif(Font.PLAIN,15),COPPER,cx,59,true);
        write(g,"공중도시의 시간 기관실",serif(Font.BOLD,24),INK,cx,104,true);
        divider(g,cx,121,55,HAIRLINE);
        g.setColor(new Color(120,94,60));
        g.draw(new Rectangle2D.Double(PRINT_X-2,PRINT_Y-2,PRINT_W+4,PRINT_H+4));
        g.setColor(new Color(135,103,60,165));
        for (int i:new int[]{0,1}) for (int j:new int[]{0,1}) {
            int x=PRINT_X+i*PRINT_W,y=(int)Math.round(PRINT_Y+j*PRINT_H);
            g.drawLine(x+(i==0?-5:5),y+(j==0?-5:5),x+(i==0?14:-14),y+(j==0?-5:5));
            g.drawLine(x+(i==0?-5:5),y+(j==0?-5:5),x+(i==0?-5:5),y+(j==0?12:-12));
        }
        engraveAirship(g,cx,376);
    }
}
