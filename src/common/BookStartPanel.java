package common;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.image.BufferedImage;
import room1.WorkshopView;

/** Team opening. Signing the register takes the player into the illustration on its first spread. */
public final class BookStartPanel extends JPanel {
    private enum Scene { SHELF, OPENING, REGISTER, ENTERING, FINISHED }
    private static final double OPEN_SECONDS = 2.15;
    private static final double ENTRY_SECONDS = 2.20;

    private final Consumer<String> onEnter;
    private final BookArt art;
    private final JButton book;
    private final JButton sign;
    private final JTextField name = new JTextField();
    private final Timer animation = new Timer(1000 / 30, e -> advance());
    private Scene scene = Scene.SHELF;
    private double elapsed;
    private long lastTick;
    private boolean needsName;
    private String passenger = "";
    private BufferedImage motionCanvas;
    private int latePaints;
    private boolean fastEnlargement;

    public BookStartPanel(Consumer<String> onEnter) {
        this.onEnter = Objects.requireNonNull(onEnter);
        art = new BookArt(readImage("library.jpg"), readImage("logbook.jpg"), WorkshopView.backdrop());
        setLayout(null);
        setBackground(new Color(15, 18, 19));
        setPreferredSize(new Dimension(BookArt.WIDTH, BookArt.HEIGHT));
        book = hotspot("책장에서 항해 일지 꺼내기", this::pullBook);
        sign = hotspot("승선 명부에 서명하기", this::enter);
        name.setOpaque(false);
        name.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
        name.setForeground(BookArt.INK);
        name.setCaretColor(BookArt.INK);
        name.setSelectionColor(new Color(214, 180, 118));
        name.setSelectedTextColor(BookArt.INK);
        name.getAccessibleContext().setAccessibleName("승선자 이름");
        name.setHorizontalAlignment(JTextField.CENTER);
        name.addActionListener(e -> enter());
        name.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { editedName(); }
            public void removeUpdate(DocumentEvent e) { editedName(); }
            public void changedUpdate(DocumentEvent e) { editedName(); }
        });
        add(name);
        add(book);
        add(sign);
        setControls();
        addHierarchyListener(event -> {
            if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) syncClock();
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ENTER"), "turn");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"), "turn");
        getActionMap().put("turn", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                if (scene == Scene.SHELF) pullBook();
                else if (scene == Scene.REGISTER) enter();
            }
        });
    }

    private static BufferedImage readImage(String filename) {
        URL file = BookStartPanel.class.getResource("/common/" + filename);
        try {
            if (file != null) return ImageIO.read(file);
            Path image = Path.of("resources", "common", filename);
            return Files.isRegularFile(image) ? ImageIO.read(image.toFile()) : null;
        } catch (IOException ignored) {
            return null;
        }
    }

    private JButton hotspot(String label, Runnable action) {
        JButton button = new JButton();
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.getAccessibleContext().setAccessibleName(label);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> action.run());
        button.getModel().addChangeListener(e -> repaint());
        button.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { repaint(); }
            public void focusLost(FocusEvent e) { repaint(); }
        });
        return button;
    }

    private void editedName() {
        needsName = false;
        repaint();
    }

    private void pullBook() {
        if (scene == Scene.SHELF) transition(Scene.OPENING);
    }

    private void enter() {
        if (scene != Scene.REGISTER) return;
        passenger = name.getText().strip();
        if (passenger.isEmpty()) {
            needsName = true;
            name.requestFocusInWindow();
            repaint();
            return;
        }
        transition(Scene.ENTERING);
    }

    private void transition(Scene next) {
        scene = next;
        elapsed = 0;
        latePaints = 0;
        fastEnlargement = false;
        setControls();
        syncClock();
        if (scene == Scene.REGISTER) name.requestFocusInWindow();
        repaint();
    }

    private void setControls() {
        book.setVisible(scene == Scene.SHELF);
        sign.setVisible(scene == Scene.REGISTER);
        name.setVisible(scene == Scene.REGISTER);
    }

    private boolean moving() {
        return scene == Scene.OPENING || scene == Scene.ENTERING;
    }

    private void advance() {
        if (!isShowing()) {
            animation.stop();
            return;
        }
        long now = System.nanoTime();
        elapsed += (now - lastTick) / 1_000_000_000.0;
        lastTick = now;
        if (scene == Scene.OPENING && elapsed >= OPEN_SECONDS) transition(Scene.REGISTER);
        else if (scene == Scene.ENTERING && elapsed >= ENTRY_SECONDS) {
            transition(Scene.FINISHED);
            onEnter.accept(passenger);
        } else repaint();
    }

    private void syncClock() {
        if (moving() && isShowing()) {
            lastTick = System.nanoTime();
            animation.start();
        } else animation.stop();
    }

    @Override public void removeNotify() {
        animation.stop();
        motionCanvas=null;
        super.removeNotify();
    }

    private double scale() {
        return Math.min(getWidth() / (double) BookArt.WIDTH, getHeight() / (double) BookArt.HEIGHT);
    }

    private int insetX(double scale) { return (int) Math.round((getWidth() - BookArt.WIDTH * scale) / 2); }
    private int insetY(double scale) { return (int) Math.round((getHeight() - BookArt.HEIGHT * scale) / 2); }

    private void place(Component control, Rectangle2D area, double scale) {
        control.setBounds(insetX(scale) + (int) Math.round(area.getX() * scale),
                insetY(scale) + (int) Math.round(area.getY() * scale),
                (int) Math.round(area.getWidth() * scale), (int) Math.round(area.getHeight() * scale));
    }

    @Override public void doLayout() {
        double s = scale();
        place(book, BookArt.SHELF_HIT, s);
        place(sign, BookArt.SIGN_HIT, s);
        place(name, BookArt.NAME_FIELD, s);
        name.setFont(BookArt.serif(Font.BOLD, Math.max(10, (int) Math.round(24 * s))));
    }

    private static boolean lit(JButton button) {
        return button.getModel().isRollover() || button.hasFocus();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        double s = scale();
        if (s <= 0) { g.dispose(); return; }
        // Composite moving paper at its native artwork scale once, rather than blending each
        // layer again across every physical screen pixel on a HiDPI monitor. The final
        // approach is drawn directly at screen resolution to match the live room.
        boolean nativeCanvas = moving() && s*g.getTransform().getScaleX()>.95
                && !(scene==Scene.ENTERING && elapsed/ENTRY_SECONDS>.94);
        if (nativeCanvas) {
            long began=System.nanoTime();
            int w=(int)Math.ceil(getWidth()/s), h=(int)Math.ceil(getHeight()/s);
            if (motionCanvas==null || motionCanvas.getWidth()!=w || motionCanvas.getHeight()!=h)
                motionCanvas=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
            Graphics2D paper=motionCanvas.createGraphics();
            paper.setColor(getBackground()); paper.fillRect(0,0,w,h);
            render(paper,w,h,1);
            paper.dispose();
            BookArt.quality(g);
            if (fastEnlargement)
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(motionCanvas,0,0,getWidth(),getHeight(),null);
            // A software-only display may miss deadlines enlarging a completed frame.
            // Keep the actual art bilinear and simplify only this last blit for the rest of the shot.
            latePaints=System.nanoTime()-began>30_000_000L ? latePaints+1 : 0;
            if (latePaints>=2) fastEnlargement=true;
        } else {
            render(g,getWidth(),getHeight(),s);
        }
        g.dispose();
    }

    private void render(Graphics2D g,int width,int height,double s) {
        g.clipRect(0,0,width,height);
        int ix=(int)Math.round((width-BookArt.WIDTH*s)/2);
        int iy=(int)Math.round((height-BookArt.HEIGHT*s)/2);
        g.translate(ix, iy);
        g.scale(s, s);
        BookArt.quality(g);
        if (scene == Scene.SHELF || scene == Scene.OPENING) {
            g.clipRect(0, 0, BookArt.WIDTH, BookArt.HEIGHT);
            art.unfold(g, scene == Scene.SHELF ? 0 : elapsed / OPEN_SECONDS, lit(book));
        } else {
            Rectangle r = WorkshopView.bounds(width, height);
            Rectangle2D target = new Rectangle2D.Double((r.x-ix)/s, (r.y-iy)/s, r.width/s, r.height/s);
            double travel = scene == Scene.FINISHED ? 1 : scene == Scene.ENTERING ? Math.min(1, elapsed/ENTRY_SECONDS) : 0;
            if (travel==0) g.clipRect(0, 0, BookArt.WIDTH, BookArt.HEIGHT);
            art.register(g, target, travel, needsName, scene==Scene.REGISTER && lit(sign),
                    scene == Scene.REGISTER ? "" : passenger);
        }
    }
}
