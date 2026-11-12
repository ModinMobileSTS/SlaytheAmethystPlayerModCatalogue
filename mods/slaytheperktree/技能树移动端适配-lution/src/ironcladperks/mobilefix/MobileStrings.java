package ironcladperks.mobilefix;

import com.megacrit.cardcrawl.core.Settings;

/** Small text table for the touch controls, kept minimal on purpose. */
public final class MobileStrings {
    private MobileStrings() {
    }

    private static boolean chinese() {
        return Settings.language == Settings.GameLanguage.ZHS
                || Settings.language == Settings.GameLanguage.ZHT;
    }

    public static String close() {
        return chinese() ? "关闭" : "CLOSE";
    }

    public static String bonusOn() {
        return chinese() ? "加成：开启" : "BONUS ON";
    }

    public static String bonusOff() {
        return chinese() ? "加成：暂停" : "BONUS OFF";
    }

    public static String modeBuy() {
        return chinese() ? "点击：购买" : "TAP: BUY";
    }

    public static String modeRemove() {
        return chinese() ? "点击：移除" : "TAP: REMOVE";
    }

    public static String bonusesPausedNote() {
        return chinese() ? "天赋加成暂停中：本局不会再获得任何天赋加成"
                : "Perk bonuses paused: no perk bonus will be granted";
    }

    public static String hint() {
        return chinese()
                ? "单指拖动可平移视图；点击天赋可购买（移除模式下则移除一级）；点右上角「关闭」或按返回键退出。"
                : "Drag with one finger to pan. Tap a perk to buy it (REMOVE mode takes a rank back). Tap CLOSE at the top right or press Back to exit.";
    }
}
