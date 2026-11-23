package common;

import javax.swing.*;
import java.awt.*;

/**
 * 설정 창: 효과음 on/off, 음량, 화면 흔들림, 시간 경고음, 힌트 사용 확인.
 * 설정을 여는 동안 제한 시간은 멈춘다.
 */
public class SettingsDialog extends DialogBox {

    public SettingsDialog(Window owner) {
        super(owner, "설정", 440, 430);
        GameSettings s = GameSettings.getInstance();
        GameManager gm = GameManager.getInstance();
        boolean pausedHere = gm.isRunning() && !gm.isPaused();
        if (pausedHere) gm.pause();

        JCheckBox sound = check("효과음 켜기", s.isSoundOn());
        JSlider volume = new JSlider(0, 100, s.getVolume());
        volume.setOpaque(false);
        volume.setMajorTickSpacing(25);
        volume.setPaintTicks(true);
        volume.setForeground(Theme.GOLD);
        JCheckBox shake = check("오답 시 화면 흔들림 / 붉은 효과", s.isScreenShake());
        JCheckBox warn = check("남은 시간 1분 이하 경고음", s.isLowTimeWarning());
        JCheckBox confirmHint = check("힌트 사용 전에 확인 창 띄우기", s.isConfirmHint());

        JPanel col = new JPanel(new GridLayout(0, 1, 0, 8));
        col.setOpaque(false);
        col.add(sound);
        col.add(Theme.label("효과음 음량", Font.PLAIN, 14, Theme.TEXT));
        col.add(volume);
        col.add(shake);
        col.add(warn);
        col.add(confirmHint);
        setContent(col);

        addButton("저장", () -> {
            s.setSoundOn(sound.isSelected());
            s.setVolume(volume.getValue());
            s.setScreenShake(shake.isSelected());
            s.setLowTimeWarning(warn.isSelected());
            s.setConfirmHint(confirmHint.isSelected());
            s.save();
            SoundManager.getInstance().play(SoundManager.Effect.ITEM);
            dispose();
        });
        addButton("취소", this::dispose);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (pausedHere) gm.resume();
            }
        });
    }

    private JCheckBox check(String text, boolean selected) {
        JCheckBox c = new JCheckBox(text, selected);
        c.setOpaque(false);
        c.setFont(Theme.font(Font.PLAIN, 15));
        c.setForeground(Theme.TEXT);
        c.setFocusPainted(false);
        return c;
    }
}
