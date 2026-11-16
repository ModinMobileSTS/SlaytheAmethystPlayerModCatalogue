package com.navsysfix;

import basemod.EasyConfigPanel;
import com.megacrit.cardcrawl.localization.UIStrings;

import java.util.HashMap;
import java.util.Map;

/**
 * The config page for this patch.
 *
 * Built with BaseMod's EasyConfigPanel - the same widget kit the game's mod list
 * config page uses (ModPanel background + ModLabeledToggleButton for booleans +
 * ModMinMaxSlider for numbers) - so all switches live in the mod's config page
 * instead of on the map.
 *
 * EasyConfigPanel reads the public static, non-final fields declared here,
 * creates a SpireConfig named "config" for MOD_ID, and writes every change back
 * immediately. ConfigRuntime.load() reads that same file at boot so the runtime
 * sees the saved values before the panel is ever opened.
 */
public class NavSysConfigPanel extends EasyConfigPanel {

    /** This patch mod's own modid; the config file is stored under it. */
    public static final String MOD_ID =
            "amethyst.ai.patch.com.dsaita.navsys.patch-1794815217126-ed53f487";

    /** Left-side NavSys route list; off by default so the map stays clean. */
    public static boolean showPanel = false;
    /** Gold line: the route that avoids combat. */
    public static boolean showSafeRoute = true;
    /** Red line: the route that seeks combat. */
    public static boolean showAggroRoute = true;
    /** Tap a map node to toggle its marker instead of travelling there. */
    public static boolean markMode = false;

    /** How strongly the route colour stains the icon (0 = invisible, 100 = solid). */
    public static int tintPercent = 70;

    public static int safeR = 255;
    public static int safeG = 215;
    public static int safeB = 0;
    public static int aggroR = 255;
    public static int aggroG = 0;
    public static int aggroB = 0;

    public NavSysConfigPanel() {
        this(MOD_ID, "config");
    }

    public NavSysConfigPanel(String modID, String configName) {
        super(modID, labels(), configName);
        // 11 rows of 50px: with the default 8px padding they would spill onto a
        // second page (and show paging arrows), so tighten the gap instead.
        setPadding(2.0f);
        setNumberRange("tintPercent", 0.0f, 100.0f);
        setNumberRange("safeR", 0.0f, 255.0f);
        setNumberRange("safeG", 0.0f, 255.0f);
        setNumberRange("safeB", 0.0f, 255.0f);
        setNumberRange("aggroR", 0.0f, 255.0f);
        setNumberRange("aggroG", 0.0f, 255.0f);
        setNumberRange("aggroB", 0.0f, 255.0f);
    }

    /** Row labels; EasyConfigPanel falls back to the field name when a key is missing. */
    private static UIStrings labels() {
        Map<String, String> dict = new HashMap<>();
        dict.put("showPanel", "显示左侧路线列表面板（默认关）");
        dict.put("showSafeRoute", "显示安全路线（金线）");
        dict.put("showAggroRoute", "显示激进路线（红线）");
        dict.put("markMode", "标记模式：点击地图节点打标记，不会移动");
        dict.put("tintPercent", "染色浓度 0-100（越高越明显，100=整个图标变色）");
        dict.put("safeR", "安全线 红色 0-255");
        dict.put("safeG", "安全线 绿色 0-255");
        dict.put("safeB", "安全线 蓝色 0-255");
        dict.put("aggroR", "激进线 红色 0-255");
        dict.put("aggroG", "激进线 绿色 0-255");
        dict.put("aggroB", "激进线 蓝色 0-255");

        UIStrings strings = new UIStrings();
        strings.TEXT = new String[0];
        strings.EXTRA_TEXT = new String[0];
        strings.TEXT_DICT = dict;
        return strings;
    }
}
