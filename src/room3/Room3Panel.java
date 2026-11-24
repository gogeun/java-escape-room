package room3;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;

/**
 * All investigation views share the same controller and eight inventory slots.
 * [통합] common.RoomPanel 을 상속해 공통 시스템(체력 / 힌트 / 인벤토리 / 진행도)과 연결된다.
 */
public final class Room3Panel extends common.RoomPanel implements AutoCloseable {
    private final GameController game=new GameController(this::playSound);
    private final Runnable onComplete;
    private final Set<JDialog> dialogs=new HashSet<>();
    private SoundManager audio;
    private boolean completionSent;
    private boolean soundEnabled=true;
    private final AssetManager assets=new AssetManager();
    private final JLabel dialogue=TextDisplay.label("붉은 달빛이 스며든다.");
    private final JPanel inventory=new JPanel(new GridLayout(1,8,8,0));
    private final JPanel screen=new JPanel(new BorderLayout());
    private static final Color GOLD=new Color(205,165,89), DARK=new Color(23,16,20);
    private final Cursor investigate=isInGame()?Cursor.getPredefinedCursor(Cursor.HAND_CURSOR):makeCursor();   // [통합] 게임 중에는 1·2번 방과 같은 손 모양 커서

    public Room3Panel() { this(()->{}); }

    /** The host owns navigation. Called once on the EDT when this room is completed. */
    public Room3Panel(Runnable onComplete) {
        super();setLayout(new BorderLayout(0,8));this.onComplete=Objects.requireNonNull(onComplete,"onComplete");setBackground(DARK);
        JPanel header=new JPanel(new BorderLayout());header.setBackground(DARK);
        JLabel title=TextDisplay.label("  혈월의 예배당  /  BLOODMOON CHAPEL");title.setForeground(GOLD);header.add(title);
        add(header,BorderLayout.NORTH);add(screen);
        JPanel bottom=new JPanel(new BorderLayout(0,8));bottom.setBackground(DARK);
        dialogue.setForeground(new Color(239,221,189));dialogue.setBorder(BorderFactory.createEmptyBorder(4,12,4,12));
        bottom.add(dialogue,BorderLayout.NORTH);inventory.setBackground(DARK);inventory.setPreferredSize(new Dimension(800,88));
        bottom.add(inventory);bottom.setBorder(BorderFactory.createEmptyBorder(0,14,12,14));add(bottom,BorderLayout.SOUTH);
        addHierarchyListener(event->{
            if((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED)==0) return;
            if(isShowing()) openAudio();else close();
        });
        refreshInventory();chapel();
        if(isInGame()) { header.setVisible(false); bottom.setVisible(false); }   // [통합] 게임 중에는 공통 상단 바 / 가방 사용 (아래 대사 · 인벤토리 줄 숨김)
    }
    public boolean isCompleted() { return game.state.gameCompleted; }
    public boolean isSoundEnabled() { return soundEnabled; }
    /** Host settings can control room audio without adding room-specific UI. Use on EDT. */
    public void setSoundEnabled(boolean enabled) {
        soundEnabled=enabled;
        if(enabled && isShowing()) openAudio();else closeAudio();
    }
    private void openAudio() { if(soundEnabled && audio==null) audio=new SoundManager(); }
    private void playSound(String name) {
        if("candle_fail".equals(name)) reportWrongAnswer("P09");   // [통합] 촛불 순서 오류 → 체력 -1
        if(isShowing() && soundEnabled && audio!=null) audio.playEffect(name); }
    private void closeAudio() { if(audio!=null) {audio.close();audio=null;} }
    /** Release this panel's audio and popups, preserving progress for later re-entry. */
    @Override public void close() {
        if(!SwingUtilities.isEventDispatchThread()) {SwingUtilities.invokeLater(this::close);return;}
        for(JDialog dialog:new ArrayList<>(dialogs)) dialog.dispose();
        closeAudio();
    }
    private static Cursor makeCursor() {
        if(GraphicsEnvironment.isHeadless()) return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
        try {
            BufferedImage icon=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);Graphics2D g=icon.createGraphics();
            g.setStroke(new BasicStroke(4));g.setColor(Color.BLACK);g.drawOval(3,3,16,16);g.drawLine(18,18,28,28);
            g.setStroke(new BasicStroke(2));g.setColor(GOLD);g.drawOval(3,3,16,16);g.drawLine(18,18,28,28);g.dispose();
            return Toolkit.getDefaultToolkit().createCustomCursor(icon,new Point(10,10),"조사");
        } catch(RuntimeException e) {return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);}
    }
    private JButton button(String text,Runnable action) {
        JButton b=new JButton(TextDisplay.text(text),TextDisplay.icon(text));b.getAccessibleContext().setAccessibleName(text);
        b.setFocusPainted(false);b.setBackground(DARK);b.setForeground(GOLD);b.addActionListener(e->action.run());return b;
    }
    private void say(String text) {syncWithGame();dialogue.setText(text);dialogue.setToolTipText(TextDisplay.tooltip(text));refreshInventory();repaint();}
    private void refreshInventory() {
        inventory.removeAll();var items=game.inventory.items();
        for(int i=0;i<8;i++) {
            JButton slot;
            if(i<items.size()) {
                var item=items.get(i);slot=button(item.title,()->{game.inventory.select(item);say(item.title+" — "+item.description);});
                BufferedImage image=assets.image(item.image);
                slot.setIcon(new ImageIcon(image.getScaledInstance((int)(46.0*image.getWidth()/image.getHeight()),46,Image.SCALE_SMOOTH)));
                slot.setHorizontalTextPosition(SwingConstants.CENTER);slot.setVerticalTextPosition(SwingConstants.BOTTOM);slot.setToolTipText(TextDisplay.tooltip(item.description));
                slot.setBorder(BorderFactory.createLineBorder(game.inventory.selected()==item?GOLD:new Color(75,53,40),2));
            } else {slot=button("·",()->{});slot.setEnabled(false);}
            inventory.add(slot);
        }
        inventory.revalidate();inventory.repaint();
    }
    private void showScene(Scene scene,boolean back) {
        screen.removeAll();
        if(isInGame()) screen.add(inGameLayer(scene,back));   // [통합] 게임 중: 돌아가기 버튼을 장면 왼쪽 위에 띄움
        else {screen.add(scene);if(back) screen.add(button("← 예배당으로 돌아가기",this::chapel),BorderLayout.SOUTH);}
        screen.revalidate();screen.repaint();
    }
    /** [통합] 게임 중 조사 장면: 장면 왼쪽 위에 방 테마 모양의 "돌아가기" 버튼을 겹쳐 띄운다 */
    private JComponent inGameLayer(Scene scene,boolean back) {
        if(!back) return scene;
        JButton backButton=new common.ThemedButton("← 돌아가기",this::chapel);
        JLayeredPane layer=new JLayeredPane() {
            @Override public void doLayout() {
                scene.setBounds(0,0,getWidth(),getHeight());
                Dimension d=backButton.getPreferredSize();backButton.setBounds(14,12,d.width,d.height);
            }
        };
        layer.add(scene,JLayeredPane.DEFAULT_LAYER);layer.add(backButton,JLayeredPane.PALETTE_LAYER);
        return layer;
    }
    private void chapel() {
        Scene s=new Scene("background.png");s.markers=true;   // [통합] 조사 가능한 곳에 돋보기 표시
        s.spot(0,.59,.33,.40,"책상 조사",this::desk);
        if(game.state.candlesSolved && game.state.safeUnlocked)
            s.spot(.88,.76,.12,.23,"의자 아래 숨겨진 금고",this::safe);
        s.spot(.47,.48,.23,.20,"제단 조사",this::altar);
        s.spot(.83,.14,.17,.61,"귀환문",()->{
            if(game.state.gameCompleted) ending();else if(game.canEnterCircuit()) circuit();else say("문은 굳게 닫혀 있다.");
        });showScene(s,false);
    }
    private void desk() {
        Scene s=new Scene("desk.png");s.markers=true;
        s.spot(.30,.78,.40,.11,"책상 서랍",()->say(game.obtainRod()?"낡은 점화봉을 얻었다.":"서랍은 비어 있다."));
        s.spot(.66,.57,.19,.17,"어린아이의 여섯 그림",this::clues);
        s.spot(.70,.03,.26,.53,"물약 선반 자세히 보기",this::potions);
        s.spot(.10,.05,.25,.48,"책상 위 다섯 촛불",this::candles);
        s.spot(.31,.30,.30,.28,"책",()->popup("책",new Scene("book.png")).setVisible(true));
        showScene(s,true);say("먼지가 내려앉아 있다.");
    }
    private void altar() {
        Scene s=new Scene("altar.png");s.markers=true;s.spot(.22,.49,.56,.35,"제단",this::useAtAltar);
        showScene(s,true);say("차가운 돌 위로 달빛이 흐른다.");
    }
    private JDialog popup(String title,JComponent content) {
        Window owner=SwingUtilities.getWindowAncestor(this);
        JDialog dialog=new JDialog(owner,TextDisplay.text(title),Dialog.ModalityType.APPLICATION_MODAL);
        dialogs.add(dialog);
        dialog.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent event) { dialogs.remove(dialog); }
        });
        JPanel body=new JPanel(new BorderLayout(8,8));body.setBackground(DARK);
        body.add(content);if(!isInGame()) body.add(button("닫기",dialog::dispose),BorderLayout.SOUTH);   // [통합] 게임 중에는 왼쪽 위 [← 돌아가기]로 닫음
        dialog.setContentPane(body);dialog.setSize(Math.min(1100,owner.getWidth()-60),Math.min(780,owner.getHeight()-60));
        dialog.setLocationRelativeTo(this);dialog.getRootPane().registerKeyboardAction(e->dialog.dispose(),KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),JComponent.WHEN_IN_FOCUSED_WINDOW);
        if(isInGame()) {   // [통합] 창 테두리 없이 방 화면 위에 뜨는 팝업 (뒤 화면 어둡게, 금색 테두리)
            common.RoomPopup.style(dialog);body.setOpaque(true);body.setBorder(BorderFactory.createLineBorder(GOLD,2));
        }
        return dialog;
    }
    private void clues() {
        int[] page={0};Scene s=new Scene("clue_00.png");JPanel panel=new JPanel(new BorderLayout());panel.add(s);
        JLabel count=TextDisplay.label("1 / 6",SwingConstants.CENTER);JPanel nav=new JPanel();
        // The supplied cumulative sequence contradicts basic.txt. Compose each figure
        // from the supplied intact/burned artwork to preserve the specified answer.
        s.overlay=g->{
            int[] disappear={1,2,5,3,4};
            for(int figure=0;figure<5;figure++) {
                boolean burned=false;for(int k=0;k<page[0];k++) if(disappear[k]==figure+1) burned=true;
                BufferedImage source=assets.image(burned?"clue_05.png":"clue_00.png");
                int left=figure*200,right=(figure+1)*200;
                g.drawImage(source,left*1600/1000,0,right*1600/1000,900,left,0,right,1200,null);
            }
        };
        Consumer<Integer> change=delta->{page[0]=(page[0]+delta+6)%6;count.setText((page[0]+1)+" / 6");s.repaint();};
        nav.add(button("← 이전",()->change.accept(-1)));nav.add(count);nav.add(button("다음 →",()->change.accept(1)));panel.add(nav,BorderLayout.SOUTH);
        popup("어린아이의 그림",panel).setVisible(true);
    }
    private void potions() {
        Scene s=new Scene("potion.png");
        popup("물약 선반",s).setVisible(true);
    }
    private void useAtAltar() {
        var selected=game.inventory.selected();
        if(selected==InventoryManager.Item.IGNITION_ROD || selected==InventoryManager.Item.MOON_CRYSTAL)
            say(game.combine());
        else say("아무 일도 일어나지 않는다.");
    }
    private void candles() {
        Scene s=new Scene("candle.png");JLabel feedback=TextDisplay.label(" ",SwingConstants.CENTER);
        double[] xs={.20,.355,.509,.665,.821},ys={.31,.49,.35,.22,.07};
        for(int i=0;i<5;i++) {int index=i+1;s.spot(xs[i]-.045,ys[i]-.06,.09,.58,"촛불 "+index,()->{String m=game.light(index);feedback.setText(m);say(m);s.repaint();});}
        s.overlay=g->{for(int i=0;i<5;i++) if(game.candles.lit(i+1)) {
            double x=xs[i]*1600,y=ys[i]*900;g.setColor(new Color(255,175,35,65));g.fill(new Ellipse2D.Double(x-40,y-60,80,100));
            g.setColor(new Color(255,210,92));g.fill(new Ellipse2D.Double(x-9,y-42,18,40));g.setColor(Color.WHITE);g.fill(new Ellipse2D.Double(x-4,y-20,8,17));}};
        JPanel panel=new JPanel(new BorderLayout());panel.add(s);panel.add(feedback,BorderLayout.SOUTH);
        popup("다섯 촛불",panel).setVisible(true);
    }
    private void safe() {
        Scene art=new Scene("vault.png");
        JLabel feedback=TextDisplay.label(" ",SwingConstants.CENTER);
        for(int i=0;i<9;i++) {int index=i;
            art.spot(.322+(i%3)*.125,.21+(i/3)*.185,.105,.145,(i/3+1)+"행 "+(i%3+1)+"열",()->{
                int before=game.safe.count();String m=game.toggleSafe(index);
                if(before==2 && game.safe.count()==3 && !game.state.moonCrystalObtained) reportWrongAnswer("P10");   // [통합] 금고 오답 → 체력 -1
                feedback.setText(m.isEmpty()?" ":m);say(m);art.repaint();
            });
        }
        art.overlay=g->{for(int i=0;i<9;i++) if(game.safe.selected(i)) {
            g.setColor(new Color(205,165,89,90));
            g.fill(new Ellipse2D.Double((.322+i%3*.125)*1600,(.21+i/3*.185)*900,.105*1600,.145*900));
        }};
        JPanel panel=new JPanel(new BorderLayout());panel.add(art);panel.add(feedback,BorderLayout.SOUTH);
        popup("금고",panel).setVisible(true);
    }
    private void circuit() {
        if(!game.canEnterCircuit()) return;
        Scene board=new Scene("circuit_board.png");JPanel panel=new JPanel(new BorderLayout());panel.add(board);
        JLabel feedback=TextDisplay.label(" ",SwingConstants.CENTER);JDialog[] dialog={null};
        for(int i=0;i<16;i++) {int index=i;board.spot(.105+(i%4)*.20,.15+(i/4)*.195,.19,.185,"타일",()->{
            if(game.circuit.fixed(index)) return;
            boolean won=game.rotateCircuit(index);board.repaint();feedback.setText(won?"귀환문의 봉인이 해제되었다.":" ");
            if(won) {dialog[0].dispose();ending();}
        });}
        board.overlay=g->{var powered=game.circuit.powered();for(int i=0;i<16;i++) {
            double x=(.105+i%4*.20)*1600,y=(.15+i/4*.195)*900,w=.19*1600,h=.185*900;
            String name="tile_"+(game.circuit.fixed(i)?"fixed_":"")+(game.circuit.corner(i)?"corner":"straight")+".png";
            Graphics2D tile=(Graphics2D)g.create();tile.translate(x+w/2,y+h/2);tile.scale(w/256,h/256);tile.rotate(game.circuit.rotation(i)*Math.PI/2);
            tile.drawImage(assets.image(name),-128,-128,256,256,null);tile.dispose();
            if(powered.contains(i)) {g.setColor(new Color(255,60,35,110));g.setStroke(new BasicStroke(4));g.draw(new Rectangle2D.Double(x+3,y+3,w-6,h-6));}
        }g.setColor(GOLD);g.setFont(new Font("Malgun Gothic",Font.BOLD,25));g.drawString("→",100,225);g.drawString("→",1460,750);};
        JPanel controls=new JPanel(new BorderLayout());controls.add(feedback);controls.add(button("되돌리기",()->{game.circuit.reset();board.repaint();feedback.setText(" ");}),BorderLayout.SOUTH);panel.add(controls,BorderLayout.SOUTH);
        dialog[0]=popup("마력 회로",panel);dialog[0].setVisible(true);
    }
    private void ending() {
        Scene s=new Scene("background.png");s.overlay=g->{g.setColor(new Color(12,6,12,210));g.fillRect(0,0,1600,900);g.setColor(GOLD);
            g.setFont(new Font("Malgun Gothic",Font.BOLD,60));g.drawString(TextDisplay.text("혈월의 예배당 — 탈출 성공"),350,410);
            g.setFont(new Font("Malgun Gothic",Font.PLAIN,27));g.drawString(TextDisplay.text("붉은 빛이 사라지고, 귀환문 너머로 새벽이 밝아온다."),430,485);};
        showScene(s,false);say("귀환문의 봉인이 해제되었다.");
        if(game.state.gameCompleted && !completionSent) {
            completionSent=true;
            SwingUtilities.invokeLater(onComplete);
        }
    }
    // ------------------------------------------------------------------
    // [통합] common.RoomPanel 연결 (체력 / 힌트 / 목표 / 진행도 / 인벤토리)
    // ------------------------------------------------------------------
    private boolean sentRod, sentCandles, sentCrystal, sentStaff, sentCircuit;

    /** 방 안의 진행 상태(GameState)가 바뀌었으면 공통 시스템에 한 번씩 알린다. */
    private void syncWithGame() {
        GameState s=game.state;
        if(s.ignitionRodObtained && !sentRod) { sentRod=true; giveRoomItem(InventoryManager.Item.IGNITION_ROD); }
        if(s.candlesSolved && !sentCandles) { sentCandles=true; reportSolved("P09"); }
        if(s.moonCrystalObtained && !sentCrystal) { sentCrystal=true; reportSolved("P10"); giveRoomItem(InventoryManager.Item.MOON_CRYSTAL); }
        if(s.staffCompleted && !sentStaff) {
            sentStaff=true; reportSolved("P11");
            if(isInGame()) { game().removeItem("R3-IGNITION_ROD"); game().removeItem("R3-MOON_CRYSTAL"); }
            giveRoomItem(InventoryManager.Item.MOON_STAFF);
        }
        if(s.circuitSolved && !sentCircuit) { sentCircuit=true; reportSolved("P12"); }
        updateProgress();
    }

    private void giveRoomItem(InventoryManager.Item item) {
        giveItem(new common.Item("R3-"+item.name(), item.title, item.description, assets.image(item.image),
            () -> { game.inventory.select(item); say(item.title+" — "+item.description); }));   // 가방에서 누르면 그 아이템 선택
    }

    @Override public String getRoomName() { return "혈월의 예배당"; }

    @Override public common.RoomTheme getTheme() { return common.RoomTheme.BLOODMOON; }   // 검은 철 + 핏빛 UI

    @Override public String getObjective() {
        switch(getHintStage()) {
            case 1: return "예배당을 둘러보자.\n책상 쪽에 쓸 만한 물건이 있을지도 모른다.";
            case 2: return "점화봉을 얻었다.\n책상 위 다섯 촛불을 올바른 순서로 밝히자.";
            case 3: return "어딘가에서 금속이 움직였다.\n새로 나타난 금고를 열자.";
            case 4: return "점화봉과 붉은 달의 수정을 모았다.\n제단에서 두 물건을 하나로 합치자.";
            case 5: return "붉은 달의 지팡이가 완성됐다.\n귀환문의 마력 회로를 이어 봉인을 풀자.";
            default: return "귀환문의 봉인이 풀렸다! 최종 탈출!";
        }
    }

    @Override public java.util.List<String> getCurrentHints() {
        // hints.txt 의 room3 힌트를 못 읽었을 때 쓰는 기본 힌트
        return Arrays.asList("화면의 물건을 클릭해 조사해 보세요.",
            "아래 인벤토리 칸을 눌러 아이템을 선택한 뒤 사용할 곳을 클릭하세요.");
    }

    @Override protected String getHintRoomKey() { return "room3"; }

    @Override protected int getHintStage() {
        GameState s=game.state;
        if(s.gameCompleted) return 6;
        if(s.staffCompleted) return 5;
        if(s.moonCrystalObtained) return 4;
        if(s.candlesSolved) return 3;
        if(s.ignitionRodObtained) return 2;
        return 1;
    }

    /** [통합] 개발자 모드: 다음 퍼즐 하나를 바로 해결 (오답 처리 없이) */
    @Override public boolean devSkipPuzzle() {
        GameState s=game.state;
        if(s.gameCompleted) return false;
        for(JDialog dialog:new ArrayList<>(dialogs)) dialog.dispose();
        if(!s.ignitionRodObtained) game.obtainRod();
        else if(!s.candlesSolved) { for(int c:new int[]{2,3,5,1,4}) if(!game.candles.lit(c)) game.light(c); }
        else if(!s.moonCrystalObtained) {
            for(int i=0;i<9;i++) if(game.safe.selected(i) && i!=2 && i!=3 && i!=7) game.toggleSafe(i);
            for(int i:new int[]{2,3,7}) if(!game.safe.selected(i)) game.toggleSafe(i);
        }
        else if(!s.staffCompleted) game.combine();
        else { s.circuitSolved=true; s.gameCompleted=true; ending(); return true; }
        chapel(); say("");
        return true;
    }

    @Override public int getTotalPuzzles() { return 4; }
    @Override public int getSolvedCount() {
        GameState s=game.state;
        return (s.candlesSolved?1:0)+(s.moonCrystalObtained?1:0)+(s.staffCompleted?1:0)+(s.circuitSolved?1:0);
    }

    private record Hotspot(Rectangle2D bounds,String title,Runnable action) {}
    private final class Scene extends JPanel {
        String background;boolean markers;Consumer<Graphics2D> overlay=g->{};final java.util.List<Hotspot> spots=new ArrayList<>();Hotspot hover;
        Scene(String background) {
            this.background=background;setLayout(null);setBackground(Color.BLACK);setPreferredSize(new Dimension(960,540));
            MouseAdapter mouse=new MouseAdapter() {
                @Override public void mouseMoved(MouseEvent e) {Point2D p=logical(e);Hotspot before=hover;hover=spots.stream().filter(s->s.bounds.contains(p)).findFirst().orElse(null);if(hover==before) return;setCursor(hover==null?Cursor.getDefaultCursor():investigate);setToolTipText(TextDisplay.tooltip(hover==null?null:hover.title));repaint();}   // [통합] 가리키는 지점이 바뀔 때만 다시 그림
                @Override public void mouseExited(MouseEvent e) {hover=null;setCursor(Cursor.getDefaultCursor());repaint();}
                @Override public void mouseClicked(MouseEvent e) {if(!SwingUtilities.isLeftMouseButton(e)) return;Point2D p=logical(e);for(Hotspot s:spots) if(s.bounds.contains(p)) {s.action.run();return;}}
            };addMouseListener(mouse);addMouseMotionListener(mouse);
        }
        void spot(double x,double y,double w,double h,String title,Runnable action) {
            Hotspot spot=new Hotspot(new Rectangle2D.Double(x*1600,y*900,w*1600,h*900),title,action);spots.add(spot);
            JButton target=new JButton();target.setOpaque(false);target.setContentAreaFilled(false);target.setBorderPainted(false);target.setFocusPainted(false);
            target.setToolTipText(TextDisplay.tooltip(title));target.getAccessibleContext().setAccessibleName(title);target.setCursor(investigate);target.addActionListener(e->action.run());
            target.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {hover=spot;repaint();}
                @Override public void mouseExited(MouseEvent e) {hover=null;repaint();}
            });add(target);
        }
        @Override public void doLayout() {
            Rectangle2D v=viewport();
            for(int i=0;i<spots.size();i++) {Rectangle2D b=spots.get(i).bounds;
                getComponent(i).setBounds((int)(v.getX()+b.getX()*v.getWidth()/1600),(int)(v.getY()+b.getY()*v.getHeight()/900),
                    (int)(b.getWidth()*v.getWidth()/1600),(int)(b.getHeight()*v.getHeight()/900));
            }
        }
        private Rectangle2D viewport() {BufferedImage im=assets.image(background);double ratio=(double)im.getWidth()/im.getHeight(),w=getWidth(),h=w/ratio;if(h>getHeight()) {h=getHeight();w=h*ratio;}return new Rectangle2D.Double((getWidth()-w)/2,(getHeight()-h)/2,w,h);}
        Point2D logical(MouseEvent e) {Rectangle2D v=viewport();return new Point2D.Double((e.getX()-v.getX())*1600/v.getWidth(),(e.getY()-v.getY())*900/v.getHeight());}
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);Rectangle2D v=viewport();Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.translate(v.getX(),v.getY());g.scale(v.getWidth()/1600,v.getHeight()/900);common.FastImage.draw(g,assets.image(background),0,0,1600,900);overlay.accept(g);   // [통합] 배경 확대본 재사용
            if(hover!=null && !markers) {g.setColor(new Color(240,197,116,45));g.fill(hover.bounds);g.setColor(GOLD);g.setStroke(new BasicStroke(2));g.draw(hover.bounds);}
            if(markers) {   // [통합] 1번 방과 같은 돋보기 표시 (화면 크기와 상관없이 같은 크기)
                double k=1600/Math.max(1,v.getWidth());
                for(Hotspot sp:spots) common.Magnifier.paint(g,sp.bounds.getCenterX(),sp.bounds.getCenterY(),common.Magnifier.SIZE*k,sp==hover);
            }
            g.dispose();
        }
    }
}
