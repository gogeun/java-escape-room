package common;

import javax.swing.*;
import java.awt.*;

/**
 * 설정 창 (상단 바 [설정] 버튼 또는 ESC).
 *
 *  - 게임 중에 열면 제한 시간이 멈추는 "일시정지 + 설정" 창이 된다.
 *      설정 항목 + 계속하기 / 저장하기 / 처음부터 다시하기 / 저장 후 타이틀로 / 게임 종료 / 개발자 모드
 *  - 타이틀 화면에서 열면 설정 항목만 보인다.
 *  - 상단 바에 이미 있는 기능(힌트 · 메모 · 도움말)은 여기에 넣지 않는다.
 *
 * 설정 값(효과음, 음량, 흔들림, 경고음, 힌트 확인)은 창을 닫을 때 저장된다.
 */
public class SettingsDialog extends DialogBox {

    private final GameSettings s = GameSettings.getInstance();
    private final JCheckBox sound, shake, warn, confirmHint;
    private final JSlider volume;

    public SettingsDialog(Window owner) {
        super(owner, GameManager.getInstance().isRunning() ? "일시정지 · 설정" : "설정",
            520, GameManager.getInstance().isRunning() ? 640 : 430);
        GameManager gm = GameManager.getInstance();
        boolean inGame = gm.isRunning();
        boolean pausedHere = inGame && !gm.isPaused();
        if (pausedHere) gm.pause();

        sound = check("효과음 켜기", s.isSoundOn());
        volume = new JSlider(0, 100, s.getVolume());
        volume.setOpaque(false);
        volume.setMajorTickSpacing(25);
        volume.setPaintTicks(true);
        volume.setForeground(Theme.GOLD);
        shake = check("오답 시 화면 흔들림 / 붉은 효과", s.isScreenShake());
        warn = check("남은 시간 1분 이하 경고음", s.isLowTimeWarning());
        confirmHint = check("힌트 사용 전에 확인 창 띄우기", s.isConfirmHint());

        JPanel options = new JPanel(new GridLayout(0, 1, 0, 6));
        options.setOpaque(false);
        options.add(sound);
        options.add(Theme.label("효과음 음량", Font.PLAIN, 14, Theme.TEXT));
        options.add(volume);
        options.add(shake);
        options.add(warn);
        options.add(confirmHint);

        if (inGame) {
            JLabel info = Theme.label("남은 시간 " + GameTimer.format(gm.getRemainingSec()) + "  (일시정지됨)",
                Font.BOLD, 15, Theme.GOLD_LIGHT);
            info.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel actions = new JPanel(new GridLayout(0, 2, 8, 8));
            actions.setOpaque(false);
            actions.add(action("저장하기", () -> {
                boolean ok = SaveManager.save(gm);
                DialogBox.message(this, "저장", ok ? "진행 상황을 저장했습니다.\n타이틀 화면의 [이어하기]로 계속할 수 있습니다."
                                                  : "저장에 실패했습니다.");
            }));
            actions.add(action("처음부터 다시하기", () -> {
                if (DialogBox.confirm(this, "다시하기", "현재 진행 상황을 버리고\n처음부터 다시 시작할까요?")) {
                    applyAndClose();
                    gm.restart();
                }
            }));
            actions.add(action("저장 후 타이틀로", () -> {
                applyAndClose();
                gm.quitToTitle(true);
            }));
            actions.add(action("게임 종료", () -> {
                if (DialogBox.confirm(this, "게임 종료", "진행 상황을 저장하고 게임을 종료할까요?")) {
                    applyAndClose();
                    SaveManager.save(gm);
                    System.exit(0);
                }
            }));

            JPanel col = new JPanel(new BorderLayout(0, 14));
            col.setOpaque(false);
            col.add(info, BorderLayout.NORTH);
            col.add(options, BorderLayout.CENTER);
            col.add(actions, BorderLayout.SOUTH);
            setContent(col);

            addButton("계속하기", this::applyAndClose);
        } else {
            setContent(options);
            addButton("저장", this::applyAndClose);
            addButton("취소", this::dispose);
        }
        addButton("개발자 모드", () -> {
            applyAndClose();   // 설정 창을 닫고 게임 위에 개발자 모드 창을 띄운다
            DevModeDialog.open(owner);
        });

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (pausedHere) gm.resume();
            }
        });
    }

    /** 설정 값을 저장하고 창을 닫는다 (닫히면 일시정지가 풀린다) */
    private void applyAndClose() {
        s.setSoundOn(sound.isSelected());
        s.setVolume(volume.getValue());
        s.setScreenShake(shake.isSelected());
        s.setLowTimeWarning(warn.isSelected());
        s.setConfirmHint(confirmHint.isSelected());
        s.save();
        dispose();
    }

    private static JButton action(String text, Runnable r) {
        JButton b = Theme.button(text, 15);
        b.addActionListener(e -> r.run());
        return b;
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
