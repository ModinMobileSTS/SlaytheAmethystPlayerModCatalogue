package estaliafix;

import basemod.BaseMod;
import basemod.ModBadge;
import basemod.ModButton;
import basemod.ModLabel;
import basemod.ModPanel;
import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.evacipated.cardcrawl.modthespire.lib.ByRef;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * 模组徽章与设置面板的显示修复：
 *
 * 1. 大头像：原模组用 200x200 的人物立绘（estalia/ui/button.png）注册模组徽章，
 *    而 basemod.ModBadge 按贴图原始尺寸 x Settings.scale 绘制，在模组列表里
 *    画出巨大头像遮挡其他条目。原设置由补丁注册为 48x48 图标，艾丝科技
 *    保留父模组注册并缩为 64x64；读取原图失败时退回透明占位图。
 *
 * 2. 设置面板排版：面板里的两个按钮（BlankButton.png 为 128 高）原本
 *    y=540 / y=418，与上下两行文字（y=610 / y=485）重叠、挤在一起。
 *    这里在徽章注册完成后（此时面板元素已全部添加）把按钮与文字重排为
 *    “文字一行、按钮一行”：
 *      生成专属事件 / 生成新增随机遗物（开关，保持原位 720 / 670）
 *      当前语音：…            文字 y=610
 *      [切换语音按钮]          y=460
 *      篝火人物大小：…         文字 y=400
 *      [切换大小按钮]          y=235
 *    全部左对齐 x=350。按钮移动后同步重建点击热区（Hitbox）。
 *
 */
public class ModBadgeFix {

    private static final String BADGE_NAME = "教皇 · 艾斯塔利亚";
    private static final String TECHNOLOGY_NAME = "艾丝科技";
    private static final String BADGE_IMAGE = "estalia/ui/button.png";
    private static final int SETTINGS_ICON_SIZE = 48;
    private static final int TECHNOLOGY_ICON_SIZE = 64;
    private static boolean registeringSettingsBadge;

    // 原面板坐标（用于识别元素）
    private static final float OLD_VOICE_BUTTON_Y = 540.0f;
    private static final float OLD_CAMPFIRE_BUTTON_Y = 418.0f;
    private static final float OLD_CAMPFIRE_LABEL_Y = 485.0f;

    // 新排版坐标
    private static final float NEW_VOICE_BUTTON_Y = 460.0f;
    private static final float NEW_CAMPFIRE_LABEL_Y = 400.0f;
    private static final float NEW_CAMPFIRE_BUTTON_Y = 235.0f;
    private static final float ELEMENT_X = 350.0f;

    @SpirePatch2(clz = BaseMod.class, method = "registerModBadge",
            paramtypez = {Texture.class, String.class, String.class, String.class, ModPanel.class})
    public static class HideOversizedBadge {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(@ByRef Texture[] t, String name, String author,
                                               String desc, ModPanel settingsPanel) {
            if (BADGE_NAME.equals(name)) {
                if (registeringSettingsBadge) return SpireReturn.Continue();
                if (settingsPanel != null) relayout(settingsPanel);
                Texture icon = downscale(name, SETTINGS_ICON_SIZE);
                registeringSettingsBadge = true;
                try {
                    BaseMod.registerModBadge(icon, name, author, desc, settingsPanel);
                } finally {
                    registeringSettingsBadge = false;
                }
                return SpireReturn.Return();
            }
            if (TECHNOLOGY_NAME.equals(name)) {
                t[0] = downscale(name, TECHNOLOGY_ICON_SIZE);
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch2(clz = ModBadge.class, method = "<ctor>",
            paramtypez = {Texture.class, float.class, float.class, String.class,
                    String.class, String.class, ModPanel.class})
    public static class SeparateBadgeHotspots {
        @SpirePostfixPatch
        public static void postfix(ModBadge __instance, Object[] __args) {
            String name = (String) __args[3];
            if (!BADGE_NAME.equals(name) && !TECHNOLOGY_NAME.equals(name)) return;
            Float badgeX = ReflectionHacks.getPrivate(__instance, ModBadge.class, "x");
            Texture texture = ReflectionHacks.getPrivate(__instance, ModBadge.class, "texture");
            ModPanel panel = ReflectionHacks.getPrivate(__instance, ModBadge.class, "modPanel");
            Hitbox hb = ReflectionHacks.getPrivate(__instance, ModBadge.class, "hb");
            if (badgeX == null || hb == null || texture == null) return;
            float newY = (TECHNOLOGY_NAME.equals(name) ? 378.0f : 306.0f) * Settings.scale;
            ReflectionHacks.setPrivate(__instance, ModBadge.class, "y", Float.valueOf(newY));
            hb.translate(badgeX.floatValue(), newY);
            System.out.println("[EstaliaFix] 设置入口 " + name + ": texture=" + texture.getWidth()
                    + "x" + texture.getHeight() + ", hitbox=" + hb.width + "x" + hb.height
                    + ", panel=" + (panel != null));
        }
    }

    private static Texture downscale(String name, int size) {
        Pixmap source = null;
        Pixmap target = null;
        try {
            Pixmap.setFilter(Pixmap.Filter.BiLinear);
            source = new Pixmap(Gdx.files.internal(BADGE_IMAGE));
            target = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            target.drawPixmap(source,
                    0, 0, source.getWidth(), source.getHeight(),
                    0, 0, size, size);
            Texture icon = new Texture(target);
            System.out.println("[EstaliaFix] " + name + " 徽章头像已缩小为 " + size + "x" + size);
            return icon;
        } catch (Exception e) {
            System.out.println("[EstaliaFix] 缩小徽章头像失败，改用透明占位图: " + e);
            Pixmap fallback = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            try {
                return new Texture(fallback);
            } finally {
                fallback.dispose();
            }
        } finally {
            if (source != null) source.dispose();
            if (target != null) target.dispose();
        }
    }

    private static void relayout(ModPanel panel) {
        try {
            ModButton voiceButton = null;
            ModButton campfireButton = null;
            ModLabel campfireLabel = null;
            for (Object element : panel.getUIElements()) {
                if (element instanceof ModButton) {
                    float y = buttonY((ModButton) element);
                    if (Math.abs(y - OLD_VOICE_BUTTON_Y) < 1.0f) {
                        voiceButton = (ModButton) element;
                    } else if (Math.abs(y - OLD_CAMPFIRE_BUTTON_Y) < 1.0f) {
                        campfireButton = (ModButton) element;
                    }
                } else if (element.getClass() == ModLabel.class) {
                    ModLabel label = (ModLabel) element;
                    if (Math.abs(label.getY() - OLD_CAMPFIRE_LABEL_Y) < 1.0f) {
                        campfireLabel = label;
                    }
                }
            }
            if (voiceButton != null) {
                moveButton(voiceButton, ELEMENT_X, NEW_VOICE_BUTTON_Y);
            }
            if (campfireLabel != null) {
                campfireLabel.setY(NEW_CAMPFIRE_LABEL_Y);
            }
            if (campfireButton != null) {
                moveButton(campfireButton, ELEMENT_X, NEW_CAMPFIRE_BUTTON_Y);
            }
            System.out.println("[EstaliaFix] 设置面板已重排：文字与按钮各占一排");
        } catch (Throwable t) {
            System.out.println("[EstaliaFix] 设置面板重排失败: " + t);
        }
    }

    private static float buttonY(ModButton button) {
        Float y = ReflectionHacks.getPrivate(button, ModButton.class, "y");
        return y == null ? 0.0f : y.floatValue() / Settings.scale;
    }

    private static void moveButton(ModButton button, float x, float y) {
        Float w = ReflectionHacks.getPrivate(button, ModButton.class, "w");
        Float h = ReflectionHacks.getPrivate(button, ModButton.class, "h");
        float width = w == null ? 128.0f : w.floatValue();
        float height = h == null ? 128.0f : h.floatValue();
        float scaledX = x * Settings.scale;
        float scaledY = y * Settings.scale;
        ReflectionHacks.setPrivate(button, ModButton.class, "x", Float.valueOf(scaledX));
        ReflectionHacks.setPrivate(button, ModButton.class, "y", Float.valueOf(scaledY));
        ReflectionHacks.setPrivate(button, ModButton.class, "hb", new Hitbox(
                scaledX + 14.0f * Settings.scale,
                scaledY + 14.0f * Settings.scale,
                (width - 28.0f) * Settings.scale,
                (height - 28.0f) * Settings.scale));
    }
}
