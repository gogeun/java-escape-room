package common;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import room1.Room1Panel;
import room2.Room2Panel;
import room3.Room3Panel;

public class MainFrame extends JFrame {
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel container = new JPanel(cardLayout);

    private final Room1Panel room1Panel;
    private final Room2Panel room2Panel;
    private final Room3Panel room3Panel;

    public MainFrame() {
        super("방탈출 게임");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        room1Panel = new Room1Panel(this::moveToRoom2);
        room2Panel = new Room2Panel(this::moveToRoom3);
        room3Panel = new Room3Panel(this::showEnding);

        container.add(room1Panel, "ROOM1");
        container.add(room2Panel, "ROOM2");
        container.add(room3Panel, "ROOM3");

        setContentPane(container);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                room3Panel.close();
            }
        });

        cardLayout.show(container, "ROOM1");
        pack();
        setLocationRelativeTo(null);
    }

    private void moveToRoom2() {
        cardLayout.show(container, "ROOM2");
        setSize(1280, 720);
        setLocationRelativeTo(null);
        room2Panel.requestFocusInWindow();
    }

    private void moveToRoom3() {
        cardLayout.show(container, "ROOM3");
        setSize(1600, 900);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        room3Panel.requestFocusInWindow();
    }

    private void showEnding() {
        JOptionPane.showMessageDialog(
            this,
            "축하합니다! 1, 2, 3번 방을 모두 통과하여 탈출에 성공했습니다!",
            "최종 탈출 성공",
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font", new Font("Malgun Gothic", Font.PLAIN, 15));
            UIManager.put("Button.font", new Font("Malgun Gothic", Font.PLAIN, 15));
            new MainFrame().setVisible(true);
        });
    }
}