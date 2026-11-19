package acustartrelics;

import com.megacrit.cardcrawl.core.Settings;

/**
 * 面板文案。按游戏语言在中文/英文之间切换，避免英文语言下用 CJK 字形导致显示为空白。
 */
public final class Texts {

    private static final boolean ZH =
            Settings.language == Settings.GameLanguage.ZHS || Settings.language == Settings.GameLanguage.ZHT;

    public static final String TITLE = ZH ? "开局遗物自选" : "Starting Relics";
    public static final String TOGGLE = ZH ? "启用开局遗物" : "Enable starting relics";
    public static final String PREV = ZH ? "上一页" : "Prev";
    public static final String NEXT = ZH ? "下一页" : "Next";
    public static final String CLEAR = ZH ? "清空" : "Clear";

    public static final String[] FILTERS = ZH
            ? new String[]{"全部", "初始", "普通", "罕见", "稀有", "商店", "BOSS", "特殊"}
            : new String[]{"All", "Starter", "Common", "Uncommon", "Rare", "Shop", "Boss", "Special"};

    public static final String BADGE_NAME = ZH ? "开局遗物自选" : "Starting Relics";
    public static final String AUTHOR = "Amethyst AI";
    public static final String BADGE_DESC = ZH
            ? "在 mod 设置里自选开局遗物，并修复父 mod 开局三蛋重复发放的问题。"
            : "Pick your starting relics in the mod panel, plus fixes for the egg grant logic.";

    private Texts() {
    }

    public static String page(int current, int total) {
        return ZH ? "第 " + current + " / " + total + " 页" : "Page " + current + " / " + total;
    }

    public static String selected(int selected, int shown) {
        return ZH ? "已选 " + selected + " 个 · 当前筛选 " + shown + " 个 · 点击图标切换"
                : "Selected " + selected + " / shown " + shown + "  ·  click an icon to toggle";
    }
}
