package common;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import room1.Room1Panel;
import room1.WorkshopView;

/** One desktop window for the team build and the independently runnable Room 1 submission. */
public final class MainFrame extends JFrame {
    public MainFrame() {
        super("시간의 서가");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setContentPane(opening(this::showRoom));
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1280, available.width), Math.min(760, available.height));
        setMinimumSize(new Dimension(Math.min(720, available.width), Math.min(480, available.height)));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                if (getContentPane() instanceof AutoCloseable room) {
                    try { room.close(); }
                    catch (Exception ex) { throw new IllegalStateException("방을 닫을 수 없습니다.", ex); }
                }
            }
        });
    }

    static BookStartPanel opening(Consumer<JPanel> display) {
        CompletableFuture.runAsync(WorkshopView::preload);
        Runnable room3 = () -> openTeamRoom("room3.Room3Panel", () -> {}, display);
        Runnable room2 = () -> openTeamRoom("room2.Room2Panel", room3, display);
        return new BookStartPanel(passenger -> display.accept(new Room1Panel(passenger, room2)));
    }

    // Feature branches can contain empty classes for rooms their teammates are building.
    private static void openTeamRoom(String className, Runnable next, Consumer<JPanel> display) {
        try {
            Class<?> room = Class.forName(className);
            if (!JPanel.class.isAssignableFrom(room)) return;
            Class<? extends JPanel> type = room.asSubclass(JPanel.class);
            display.accept(type.getConstructor(Runnable.class).newInstance(next));
        } catch (ClassNotFoundException absentInStandaloneBuild) {
            // Keep the completed room's arrival/ending scene in a partial build.
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("팀 방을 열 수 없습니다. " + className, ex);
        }
    }

    private void showRoom(JPanel room) {
        setContentPane(room);
        revalidate();
        repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainFrame().setVisible(true));
    }
}
