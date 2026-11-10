package room3;

import javax.swing.*;
public final class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font",new java.awt.Font("Malgun Gothic",0,15));
            UIManager.put("Button.font",new java.awt.Font("Malgun Gothic",0,15));
            JFrame frame=new JFrame(TextDisplay.text("혈월의 예배당 · Bloodmoon Chapel"));
            Room3Panel room=new Room3Panel();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent event) { room.close(); }
            });
            frame.setContentPane(room);frame.setSize(1600,900);
            frame.setMinimumSize(new java.awt.Dimension(960,640));frame.setLocationRelativeTo(null);frame.setVisible(true);
        });
    }
}
