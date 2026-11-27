package estaliafix;

import basemod.BaseMod;
import basemod.ModBadge;
import basemod.ModButton;
import basemod.ModLabel;
import basemod.ModLabeledButton;
import basemod.ModPanel;
import basemod.ReflectionHacks;
import basemod.helpers.UIElementModificationHelper;
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
 * 模组徽章显示修复：原模组注册徽章时贴图尺寸过大，会遮挡模组列表里的其他条目。
 * 原模组设置徽章由补丁缩为 48x48 图标，艾丝科技徽章缩为 64x64；
 * 读取原图失败时退回透明占位图。设置面板只调整语音按钮：把它压到与倍数
 * 选项框相同的高度并下移到说明文字下方，行距参照“人物大小 / 倍数按钮”，
 * 其余元素保持父模组原样。
 */
public class ModBadgeFix {

    private static final String BADGE_NAME = "教皇 · 艾斯塔利亚";
    private static final String TECHNOLOGY_NAME = "艾丝科技";
    private static final String BADGE_IMAGE = "estalia/ui/mod_badge.png";
    private static final int SETTINGS_ICON_SIZE = 48;
    private static final int TECHNOLOGY_ICON_SIZE = 64;
    private static boolean registeringSettingsBadge;

    @SpirePatch2(clz = BaseMod.class, method = "registerModBadge",
            paramtypez = {Texture.class, String.class, String.class, String.class, ModPanel.class})
    public static class HideOversizedBadge {
        @SpirePrefixPatch
        public static SpireReturn<Void> prefix(@ByRef Texture[] t, String name, String author,
                                               String desc, ModPanel settingsPanel) {
            if (BADGE_NAME.equals(name)) {
                if (registeringSettingsBadge) return SpireReturn.Continue();
                fixVoiceButton(settingsPanel);
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

    /**
     * 只处理“当前语音”这一行。原按钮是 128 高的 BlankButton.png，压在说明文字上；
     * 这里把它压到与倍数按钮同高，并放到说明文字下方，间距沿用人物大小那一排。
     * 说明性 ModLabel 初始文本为空（每帧的回调才填字），所以按原始 y 坐标识别。
     */
    private static void fixVoiceButton(ModPanel panel) {
        if (panel == null) {
            return;
        }
        try {
            ModButton voiceButton = null;
            ModLabel voiceLabel = null;
            ModLabel sizeLabel = null;
            ModLabeledButton sizeButton = null;
            for (Object element : panel.getUIElements()) {
                if (element instanceof ModLabeledButton) {
                    if (sizeButton == null) {
                        sizeButton = (ModLabeledButton) element;
                    }
                } else if (element instanceof ModLabel) {
                    ModLabel label = (ModLabel) element;
                    float y = label.getY();
                    String text = label.text == null ? "" : label.text;
                    if (text.contains("当前语音") || text.isEmpty() && Math.abs(y - 610.0f) < 1.0f) {
                        voiceLabel = label;
                    } else if (text.contains("人物大小") || text.isEmpty() && Math.abs(y - 485.0f) < 1.0f) {
                        sizeLabel = label;
                    }
                } else if (element.getClass() == ModButton.class && voiceButton == null) {
                    voiceButton = (ModButton) element;
                }
            }
            if (voiceButton == null) {
                return;
            }
            Float oldHeight = ReflectionHacks.getPrivate(voiceButton, ModButton.class, "h");
            if (oldHeight == null) {
                return;
            }
            float newHeight = 46.0f;
            float gap = 20.0f;
            if (sizeButton != null) {
                Float h = ReflectionHacks.getPrivate(sizeButton, ModLabeledButton.class, "h");
                if (h != null && h.floatValue() > 0.0f) {
                    newHeight = h.floatValue() / Settings.scale;
                }
            }
            if (voiceLabel != null && sizeLabel != null && sizeButton != null) {
                float measured = sizeLabel.getY() - (sizeButton.getY() + newHeight);
                if (measured > 4.0f && measured < 120.0f) {
                    gap = measured;
                }
            }
            float newY = voiceLabel != null ? voiceLabel.getY() - gap - newHeight : 540.0f;
            Float oldY = ReflectionHacks.getPrivate(voiceButton, ModButton.class, "y");
            if (oldY != null && Math.abs(oldY.floatValue() / Settings.scale - newY) < 0.5f
                    && Math.abs(oldHeight.floatValue() - newHeight) < 0.5f) {
                return;
            }
            Float x = ReflectionHacks.getPrivate(voiceButton, ModButton.class, "x");
            float scaledX = x == null ? 350.0f * Settings.scale : x.floatValue();
            float scaledY = newY * Settings.scale;
            ReflectionHacks.setPrivate(voiceButton, ModButton.class, "x", Float.valueOf(scaledX));
            ReflectionHacks.setPrivate(voiceButton, ModButton.class, "y", Float.valueOf(scaledY));
            ReflectionHacks.setPrivate(voiceButton, ModButton.class, "h", Float.valueOf(newHeight));
            Hitbox hb = ReflectionHacks.getPrivate(voiceButton, ModButton.class, "hb");
            if (hb != null) {
                hb.height = Math.max(4.0f, newHeight - 28.0f) * Settings.scale;
                UIElementModificationHelper.moveHitboxByOriginalParameters(hb,
                        scaledX + 14.0f * Settings.scale,
                        scaledY + 14.0f * Settings.scale);
            }
            System.out.println("[EstaliaFix] 语音按钮已与倍数按钮同高并单独成行: h=" + newHeight + ", y=" + newY);
        } catch (Throwable t) {
            System.out.println("[EstaliaFix] 语音按钮排版失败: " + t);
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

}
