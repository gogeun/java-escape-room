package common;

import java.awt.*;

/**
 * 도움말: 게임 규칙과 조작법. 보는 동안 시간은 멈춘다.
 */
public class HelpDialog extends DialogBox {

    private static final String TEXT =
        "■ 게임 목표\n" +
        "  세 개의 방에 숨겨진 퍼즐을 풀고 제한 시간 안에 탈출하세요.\n\n" +
        "■ 체력\n" +
        "  퍼즐에서 오답을 내면 체력(하트)이 1 줄어듭니다.\n" +
        "  체력이 0이 되면 게임 오버입니다.\n\n" +
        "■ 제한 시간\n" +
        "  화면 상단의 타이머가 0이 되면 게임 오버입니다.\n" +
        "  5분 이하는 주황색, 1분 이하는 빨간색으로 깜빡입니다.\n\n" +
        "■ 힌트\n" +
        "  막히면 힌트를 열 수 있습니다. 힌트는 현재 단계에 맞게 약한 힌트부터 공개되며,\n" +
        "  열 때마다 힌트 1개와 시간이 차감됩니다. (이미 본 힌트는 무료로 다시 보기)\n\n" +
        "■ 인벤토리 / 메모장\n" +
        "  획득한 아이템은 오른쪽 가방에 들어가며, 클릭하면 다시 볼 수 있습니다.\n" +
        "  메모장에 단서를 적어둘 수 있습니다.\n\n" +
        "■ 저장\n" +
        "  [저장] 버튼 또는 방을 이동할 때 자동으로 저장되며, 타이틀에서 [이어하기]로 계속할 수 있습니다.\n\n" +
        "■ 점수\n" +
        "  남은 시간 · 남은 체력 · 해결한 퍼즐 수가 많을수록, 힌트 · 오답이 적을수록 높습니다.\n" +
        "  난이도별 배율: 쉬움 x0.8 / 보통 x1.0 / 어려움 x1.5\n\n" +
        "■ 단축키\n" +
        "  ESC : 일시정지      H : 힌트      M : 메모장\n" +
        "  I : 가방 첫 아이템 보기      F5 : 저장";

    public HelpDialog(Window owner) {
        super(owner, "도움말", 640, 560);
        GameManager gm = GameManager.getInstance();
        boolean pausedHere = gm.isRunning() && !gm.isPaused();
        if (pausedHere) gm.pause();

        setContent(textArea(TEXT, 14));
        addButton("닫기", this::dispose);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosed(java.awt.event.WindowEvent e) {
                if (pausedHere) gm.resume();
            }
        });
    }
}
