package room1;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import javax.swing.*;
import room1.RoomProgress.Item;
import room1.RoomProgress.Station;

/** Interactive close-ups share the room state and hand new finds to its inventory. */
final class ExplorationPanel extends JPanel {
    private static final int W=680, H=340;
    private static final Color BRONZE=new Color(125,87,47), LIGHT=new Color(240,203,131), DARK=new Color(32,27,23);
    private static final Rectangle GLASS=new Rectangle(58,46,564,247);
    private static final class Background {
        static final BufferedImage IMAGE=RoomMedia.picture("workshop.jpg");
    }

    private enum Action {
        DRAWER("서랍 손잡이"), HANDLE("밸브 손잡이"), CHART("정비표"), CHAIN("점검등 당김줄"),
        TAG("정비 표찰"), LID("전성관 뚜껑"), TOKEN("승선 기념패"), WIPE("유리 닦기");
        final String title;
        Action(String title) { this.title=title; }
    }
    private record Hotspot(Action action, Rectangle area) {}

    private final Station station;
    private final RoomProgress game;
    private final Consumer<Item> collect;
    private final Consumer<String> message;
    private final Consumer<String> playSound;
    private final Timer animation;
    private BufferedImage frost;
    private final boolean[] wiped=new boolean[14*6];
    private boolean discovered, pulling, dragged;
    private int pullY, previousX, previousY, keyboardWipe;
    private Action hover, pressed;
    private double motion, target, startPosition, duration, grabPosition;
    private double chainExtension, recoilStart;
    private long motionStarted, recoilStarted, transmissionStarted;
    private boolean settlingFromDrag;
    private final InspectionArt art;

    ExplorationPanel(Station station, RoomProgress game, Consumer<Item> collect, Consumer<String> message,
                     Consumer<String> playSound) {
        this.station=station;
        this.game=game;
        this.collect=collect;
        this.message=message;
        this.playSound=playSound;
        art=station==Station.WINDOW ? null : new InspectionArt(station);
        setBackground(DARK);
        setPreferredSize(new Dimension(W,H));
        setMinimumSize(new Dimension(290,145));
        setFocusable(true);
        getAccessibleContext().setAccessibleName(station.name);
        getAccessibleContext().setAccessibleDescription("방향키로 조사할 물건을 고르고 Enter로 조작할 수 있습니다.");
        if (station==Station.WINDOW) frost=makeFrost();
        if (station==Station.DESK && (game.has(Item.CHART)||game.has(Item.HANDLE))
                || station==Station.LAMP && game.has(Item.TAG)
                || station==Station.TUBE && game.has(Item.TOKEN)) motion=target=1;
        animation=new Timer(1000/30,e->animate());
        animation.setCoalesce(true);
        MouseAdapter mouse=new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { updateHover(logical(e)); }
            @Override public void mouseExited(MouseEvent e) { if (!pulling) updateHover(new Point(-1,-1)); }
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                requestFocusInWindow();
                Point pos=logical(e);
                updateHover(pos);
                pressed=hover;
                dragged=false;
                if (hover==Action.WIPE) {
                    pulling=true;
                    wipe(pos.x,pos.y,pos.x,pos.y);
                    previousX=pos.x; previousY=pos.y;
                } else if (hover==Action.DRAWER || (hover==Action.CHAIN && game.solved()>0 && target==0)) {
                    pulling=true;
                    pullY=pos.y;
                    grabPosition=hover==Action.DRAWER ? motion : chainExtension;
                    if (hover==Action.DRAWER) motionStarted=0;
                    else recoilStarted=0;
                }
            }
            @Override public void mouseDragged(MouseEvent e) {
                if (!pulling) return;
                Point pos=logical(e);
                if (station==Station.WINDOW) {
                    wipe(previousX,previousY,pos.x,pos.y);
                    previousX=pos.x; previousY=pos.y;
                } else if (station==Station.LAMP) {
                    chainExtension=clamp(grabPosition+(pos.y-pullY)/(double)InspectionArt.CHAIN_TRAVEL);
                    dragged|=Math.abs(pos.y-pullY)>3;
                    repaint();
                } else if (station==Station.DESK) {
                    if (!dragged && Math.abs(pos.y-pullY)>4) {
                        dragged=true;
                        playSound.accept("drawer-slide");
                    }
                    if (dragged) motion=clamp(grabPosition+(pos.y-pullY)/(double)InspectionArt.DRAWER_TRAVEL);
                    repaint();
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                Point pos=logical(e);
                if (pulling) {
                    pulling=false;
                    if (station==Station.LAMP) {
                        chainExtension=clamp(grabPosition+(pos.y-pullY)/(double)InspectionArt.CHAIN_TRAVEL);
                        if (chainExtension>=.74) lightLamp();
                        else message.accept("황동 고리가 끝에 걸릴 때까지 조금 더 당겨 보자.");
                        releaseChain();
                    } else if (station==Station.DESK) {
                        double diff=motion-grabPosition;
                        double destination=dragged ? (Math.abs(diff)>.19 ? (diff>0 ? 1 : 0) : target) : 1-target;
                        slideDrawer(destination,dragged);
                    }
                } else {
                    Action selected=hit(pos);
                    if (selected!=null && selected==pressed) activate(selected);
                }
                pressed=null;
                updateHover(pos);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        bind("RIGHT",()->select(1)); bind("LEFT",()->select(-1));
        bind("ENTER",this::activateFocused); bind("SPACE",this::activateFocused);
    }

    private void bind(String key, Runnable action) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key),key);
        getActionMap().put(key,new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    private void select(int delta) {
        List<Hotspot> spots=spots();
        if (spots.isEmpty()) return;
        int i=0;
        while (i<spots.size() && spots.get(i).action()!=hover) i++;
        hover=spots.get(i==spots.size()?0:Math.floorMod(i+delta,spots.size())).action();
        getAccessibleContext().setAccessibleDescription(hover.title + " 선택됨. Enter로 조작합니다.");
        repaint();
    }

    private void activateFocused() {
        if (hover==null && !spots().isEmpty()) select(1);
        if (hover!=null) activate(hover);
    }

    private void updateHover(Point point) {
        Action next=hit(point);
        if (next!=hover) {
            hover=next;
            setCursor(Cursor.getPredefinedCursor(hover==null?Cursor.DEFAULT_CURSOR:Cursor.HAND_CURSOR));
            repaint();
        }
    }

    private Action hit(Point p) {
        for (Hotspot s:spots()) if (s.area().contains(p)) return s.action();
        return null;
    }

    private Point logical(MouseEvent e) {
        double scale=scale();
        return new Point((int)Math.floor((e.getX()-(getWidth()-W*scale)/2)/scale),
                (int)Math.floor((e.getY()-(getHeight()-H*scale)/2)/scale));
    }

    private double scale() { return Math.max(.001,Math.min(getWidth()/(double)W,getHeight()/(double)H)); }

    private List<Hotspot> spots() {
        List<Hotspot> list=new ArrayList<>(3);
        switch(station) {
            case DESK -> {
                list.add(new Hotspot(Action.DRAWER,InspectionArt.drawerGrip(motion)));
                if (motion>.89) {
                    if (!game.has(Item.HANDLE)) list.add(new Hotspot(Action.HANDLE,InspectionArt.HANDLE));
                    if (!game.has(Item.CHART)) list.add(new Hotspot(Action.CHART,InspectionArt.CHART));
                }
            }
            case LAMP -> {
                list.add(new Hotspot(Action.CHAIN,InspectionArt.chainGrip(chainExtension)));
                if (motion>.9 && target==1 && !game.has(Item.TAG))
                    list.add(new Hotspot(Action.TAG,InspectionArt.TAG));
            }
            case TUBE -> {
                list.add(new Hotspot(Action.LID,InspectionArt.lidGrip(motion)));
                if (motion>.9 && transmissionAge()>=1 && !game.has(Item.TOKEN)) list.add(new Hotspot(Action.TOKEN,InspectionArt.TOKEN));
            }
            case WINDOW -> list.add(new Hotspot(Action.WIPE,GLASS));
            default -> {}
        }
        return list;
    }

    private void activate(Action action) {
        switch(action) {
            case DRAWER -> slideDrawer(1-target,false);
            case CHAIN -> {
                if (game.solved()==0) message.accept("새는 증기가 너무 뜨겁다. 압력이 내려가면 줄을 잡을 수 있겠다.");
                else if (target==1) message.accept("점검등 불빛이 벽의 정비 표찰에 닿아 있다.");
                else {
                    chainExtension=1;
                    lightLamp();
                    releaseChain();
                }
            }
            case LID -> {
                startMotion(1-target,.76,false);
                if (target==1) {
                    transmissionStarted=System.nanoTime();
                    playSound.accept("tube-call");
                    message.accept("전성관 너머의 전언. “다음 당직자에게 내 승선 기념패를 남긴다.”");
                } else transmissionStarted=0;
            }
            case HANDLE -> collect.accept(Item.HANDLE);
            case CHART -> collect.accept(Item.CHART);
            case TAG -> collect.accept(Item.TAG);
            case TOKEN -> collect.accept(Item.TOKEN);
            case WIPE -> {
                int y=88+keyboardWipe++%4*51;
                wipe(172,y,490,y);
            }
        }
        hover=null;
        repaint();
    }

    private void lightLamp() {
        playSound.accept("lamp-pull");
        startMotion(1,.55,false);
        message.accept("점검등이 켜졌다. 배관에 걸린 황동 표찰의 글씨가 보인다.");
    }

    private static double clamp(double value) { return Math.max(0,Math.min(1,value)); }

    private void slideDrawer(double dest,boolean direct) {
        if (!direct) playSound.accept("drawer-slide");
        startMotion(dest, .17+.59*Math.abs(dest-motion), direct);
    }

    private void releaseChain() {
        recoilStarted=System.nanoTime();
        recoilStart=chainExtension;
        animation.start();
    }

    private double transmissionAge() {
        return transmissionStarted==0 ? 3 : (System.nanoTime()-transmissionStarted)/1e9;
    }

    private void startMotion(double dest,double seconds,boolean direct) {
        target=dest;
        startPosition=motion;
        motionStarted=System.nanoTime();
        duration=seconds;
        settlingFromDrag=direct;
        animation.start();
        repaint();
    }

    private void animate() {
        long now=System.nanoTime();
        if (motionStarted!=0) {
            double u=clamp((now-motionStarted)/1e9/duration);
            double eased=settlingFromDrag ? 1-Math.pow(1-u,3) : u*u*(3-2*u);
            motion=startPosition+(target-startPosition)*eased;
            if (u>=1) {
                motion=target;
                motionStarted=0;
                if (station==Station.DESK) playSound.accept("drawer-stop");
            }
        }
        if (recoilStarted!=0) {
            double t=(now-recoilStarted)/1e9;
            chainExtension=recoilStart*Math.exp(-5.4*t)*Math.cos(12*t);
            if (t>1.05) { recoilStarted=0; chainExtension=0; }
        }
        if (motionStarted==0 && recoilStarted==0 && transmissionAge()>1.65) animation.stop();
        repaint();
    }

    @Override public void removeNotify() {
        animation.stop();
        motion=target;
        motionStarted=recoilStarted=transmissionStarted=0;
        chainExtension=0;
        pulling=false;
        pressed=null;
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics.create();
        Brass.smooth(g);
        double scale=scale();
        g.translate((getWidth()-W*scale)/2,(getHeight()-H*scale)/2);
        g.scale(scale,scale);
        g.clipRect(0,0,W,H);
        switch(station) {
            case DESK -> {
                art.drawer(g,motion,!game.has(Item.HANDLE),!game.has(Item.CHART));
                caption(g,motion<.86 ? "황동 손잡이를 아래로 당기세요." :
                        game.has(Item.HANDLE)&&game.has(Item.CHART) ? "서랍 안쪽에 닳은 자국만 남았다." : "서랍 속 물건을 눌러 챙기세요.");
            }
            case LAMP -> {
                art.lamp(g,motion,chainExtension,game.solved()==0,!game.has(Item.TAG));
                caption(g,game.solved()==0 ? "배관에서 뜨거운 증기가 새어 나온다." : target==0 ? "고리가 끝에 걸릴 때까지 아래로 당기세요." :
                        game.has(Item.TAG) ? "따뜻한 불빛이 배관 벽을 비추고 있다." : "불빛 아래의 정비 표찰을 눌러 읽으세요.");
            }
            case TUBE -> {
                art.tube(g,motion,transmissionAge(),!game.has(Item.TOKEN));
                caption(g,target==0 ? "전성관의 덮개를 열어 보세요." : game.has(Item.TOKEN) ?
                        "관 너머에서 멀리 바람 소리가 들린다." : transmissionAge()<1 ?
                        "관 안쪽이 짧게 울리고, 수신구에서 무언가 굴러온다." : "수신 접시에 떨어진 승선 기념패를 챙기세요.");
            }
            case WINDOW -> window(g);
            default -> {}
        }
        if (!pulling && hover!=null && hover!=Action.WIPE) {
            for (Hotspot s:spots()) if (s.action()==hover) {
                Rectangle r=s.area();
                g.setColor(new Color(255,221,148,175));
                g.setStroke(new BasicStroke(1.5f));
                int arm=6;
                for (int x : new int[]{r.x,r.x+r.width}) for (int y : new int[]{r.y,r.y+r.height}) {
                    g.drawLine(x,y,x+(x==r.x ? arm : -arm),y);
                    g.drawLine(x,y,x,y+(y==r.y ? arm : -arm));
                }
                break;
            }
        }
        g.dispose();
    }

    private static void background(Graphics2D g,int x,int y,int w,int h,int darken) {
        g.drawImage(Background.IMAGE,0,0,W,H,x,y,x+w,y+h,null);
        g.setColor(new Color(18,16,13,darken)); g.fillRect(0,0,W,H);
        g.setPaint(new GradientPaint(0,230,new Color(20,16,13,0),0,H,new Color(18,14,12,197)));
        g.fillRect(0,200,W,H-200);
    }

    private static void caption(Graphics2D g,String text) {
        g.setFont(Brass.font(Font.PLAIN,14));
        int width=g.getFontMetrics().stringWidth(text);
        g.setColor(new Color(23,23,20,224)); g.fillRoundRect((W-width)/2-13,308,width+26,26,12,12);
        g.setColor(Brass.CREAM); g.drawString(text,(W-width)/2,327);
    }

    private static void screw(Graphics2D g,int x,int y,int r) {
        g.setPaint(new GradientPaint(x-r,y-r,LIGHT,x+r,y+r,BRONZE)); g.fillOval(x-r,y-r,r*2,r*2);
        g.setColor(DARK); g.drawLine(x-r/2,y+r/2,x+r/2,y-r/2);
    }

    private static BufferedImage makeFrost() {
        BufferedImage img=new BufferedImage(W,H,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=img.createGraphics();Brass.smooth(g);
        g.setPaint(new GradientPaint(50,50,new Color(224,235,225,235),625,290,new Color(173,202,202,221)));
        g.fill(GLASS);
        Random random=new Random(9338);
        for(int i=0;i<950;i++) {
            int x=GLASS.x+random.nextInt(GLASS.width),y=GLASS.y+random.nextInt(GLASS.height);
            int d=2+random.nextInt(13);
            g.setColor(new Color(243,251,245,30+random.nextInt(86)));
            g.drawLine(x,y,x+d,y-d); if(i%3==0) g.drawLine(x+d/2,y-d/2,x+d/2,y-d);
        }
        g.dispose();
        return img;
    }

    private void wipe(int x0,int y0,int x1,int y1) {
        Graphics2D erase=frost.createGraphics();Brass.smooth(erase);
        erase.setComposite(AlphaComposite.Clear);
        erase.setStroke(new BasicStroke(44,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        erase.drawLine(x0,y0,x1,y1);erase.dispose();
        if (!discovered) {
            int count=0;
            for(int i=0;i<wiped.length;i++) {
                int x=78+i%14*40,y=65+i/14*40;
                if(Line2D.ptSegDist(x0,y0,x1,y1,x,y)<23) wiped[i]=true;
                if(wiped[i]) count++;
            }
            if(count>=15) {
                discovered=true;
                message.accept("구름 아래에 떠 있는 섬들과 정박 중인 비행선이 모습을 드러냈다.");
            }
        }
        repaint();
    }

    private void window(Graphics2D g) {
        background(g,54,39,268,264,172);
        g.setPaint(new GradientPaint(52,36,LIGHT,615,299,new Color(98,73,44)));g.fillRoundRect(45,33,590,272,20,20);
        g.setColor(DARK);g.fillRoundRect(52,39,576,260,17,17);
        Shape clip=g.getClip();g.clip(new RoundRectangle2D.Double(GLASS.x,GLASS.y,GLASS.width,GLASS.height,14,14));
        g.drawImage(Background.IMAGE,GLASS.x,GLASS.y,GLASS.x+GLASS.width,GLASS.y+GLASS.height,105,49,301,205,null);
        g.drawImage(frost,0,0,null);g.setClip(clip);
        for(int x:new int[]{53,625}) for(int y:new int[]{44,294}) screw(g,x,y,4);
        caption(g,discovered?"창 너머 공중도시가 구름 위로 펼쳐진다.":"창유리를 마우스로 문질러 성에를 닦아 보세요.");
    }
}
