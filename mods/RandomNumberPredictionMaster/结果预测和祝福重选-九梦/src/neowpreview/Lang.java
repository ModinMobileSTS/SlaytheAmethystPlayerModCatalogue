package neowpreview;

import com.megacrit.cardcrawl.core.Settings;

/** 极少量界面文本，按当前语言给出中文或英文。 */
public final class Lang {

    private Lang() {
    }

    public static boolean zh() {
        String l = String.valueOf(Settings.language);
        return "ZHS".equals(l) || "ZHT".equals(l);
    }

    public static String undoOption() {
        // 原版事件选项文本自带方括号，这样才和原版格式一致
        return zh() ? "[更改祝福]" : "[Change Blessing]";
    }

    public static String alwaysFour() {
        return zh() ? "涅奥始终有四个选项" : "Neow always offers 4 options";
    }

    public static String on() {
        return zh() ? "开" : "ON";
    }

    public static String off() {
        return zh() ? "关" : "OFF";
    }

    public static String resetDefault() {
        return zh() ? "重置为默认" : "Reset to default";
    }

    public static String sliderImage() {
        return zh() ? "图片大小" : "Image size";
    }

    public static String curseYes() {
        return zh() ? "会被诅咒" : "Cursed";
    }

    public static String curseNo() {
        return zh() ? "不会被诅咒" : "Not cursed";
    }

    public static String hpLeft(int hp) {
        return zh() ? "剩 " + hp + " 血" : hp + " HP left";
    }

    public static String win() {
        return zh() ? "赢" : "Win";
    }

    public static String lose() {
        return zh() ? "输" : "Lose";
    }

    public static String ambushed() {
        return zh() ? "伏击" : "Ambush";
    }

    public static String adventurerGold() {
        return zh() ? "30 金币" : "30 gold";
    }

    public static String adventurerNothing() {
        return zh() ? "空手" : "Nothing";
    }

    public static String adventurerRelic() {
        return zh() ? "遗物" : "Relic";
    }

    public static String oozeBody(int hp, int attempt) {
        return zh() ? "剩 " + hp + " 血，需再伸手 " + attempt + " 次"
                : hp + " HP left, reach in " + attempt + " more";
    }

    public static String tomeBody(int hp, int reads) {
        return zh() ? "剩 " + hp + " 血，需再读 " + reads + " 次"
                : hp + " HP left, " + reads + " more reads";
    }

    public static String upgradeLine(String cardName) {
        return zh() ? cardName + " 被升级" : cardName + " will be upgraded";
    }

    public static String noUpgradable() {
        return zh() ? "没有可升级的牌" : "No upgradable card";
    }

    public static String skullRemaining(int runs) {
        return zh() ? "还能拿 " + runs + " 次" : runs + " more";
    }

    public static String modName() {
        return zh() ? "结果预测和祝福重选" : "Outcome Preview & Blessing Reroll";
    }

    public static String modDesc() {
        return zh()
                ? "悬停显示随机结果（涅奥祝福、变牌、随机遗物、篝火、事件），并支持涅奥祝福撤回重选。"
                : "Hover to see random outcomes (Neow, transforms, relics, campfire, events); Neow blessings can be undone and re-picked.";
    }
}
