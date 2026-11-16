package amethyst.translation.kokoro;

import basemod.BaseMod;
import basemod.ModBadge;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 父模组在 receivePostInitialize 中硬编码了英文的设置面板按钮文本，
 * 并以英文名和英文描述注册模组徽章。本补丁在其执行完毕后，
 * 将面板按钮与徽章提示替换为中文。仅在简体中文环境下生效。
 */
@SpirePatch2(
    cls = "Kokoro.KokoroMod",
    method = "receivePostInitialize",
    paramtypez = {}
)
public final class TranslateSettingsPanelPatch {
    public static final Logger logger = LogManager.getLogger(TranslateSettingsPanelPatch.class.getName());

    private static final String KEYWORD_ICONS_EN = "Enable Keyword Icons";
    private static final String KEYWORD_ICONS_ZH = "启用关键词图标";
    private static final String MASK_EN = "Enable Mask";
    private static final String MASK_ZH = "启用面具（角色立绘戴面具）";
    private static final String UNLOCK_EN = "Unlock Cards and Relics. Cannot be undone! (Restart or enter and leave a game for the Compendium to update.)";
    private static final String UNLOCK_ZH = "解锁全部卡牌与遗物，无法撤销！（重启游戏或进出一次对局后，图鉴会更新。）";

    private static final String BADGE_NAME_EN = "Kokoro";
    private static final String BADGE_NAME_ZH = "能乐舞者 (Kokoro)";
    private static final String BADGE_TIP_ZH = "NCS NL 来自东方Project的秦心（能乐舞者）角色模组。";

    private TranslateSettingsPanelPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(Kokoro.KokoroMod __instance) {
        if (!Settings.GameLanguage.ZHS.equals(Settings.language)) {
            return;
        }
        translateBadges();
    }

    private static void translateBadges() {
        try {
            Object badgesObj = ReflectionHacks.getPrivateStatic(BaseMod.class, "modBadges");
            if (!(badgesObj instanceof ArrayList)) {
                return;
            }
            ArrayList<?> badges = (ArrayList<?>) badgesObj;
            for (Object obj : badges) {
                if (!(obj instanceof ModBadge)) {
                    continue;
                }
                ModBadge badge = (ModBadge) obj;
                Object nameObj = ReflectionHacks.getPrivate(badge, ModBadge.class, "modName");
                if (nameObj == null || !BADGE_NAME_EN.equals(nameObj)) {
                    continue;
                }
                ReflectionHacks.setPrivate(badge, ModBadge.class, "modName", BADGE_NAME_ZH);
                ReflectionHacks.setPrivate(badge, ModBadge.class, "tip", BADGE_TIP_ZH);
                translatePanel(badge);
            }
        } catch (Exception e) {
            logger.error("Failed to translate mod badge / settings panel", e);
        }
    }

    private static void translatePanel(ModBadge badge) {
        Object panelObj = ReflectionHacks.getPrivate(badge, ModBadge.class, "modPanel");
        if (!(panelObj instanceof ModPanel)) {
            return;
        }
        ModPanel panel = (ModPanel) panelObj;
        int changed = 0;
        for (Object elemObj : panel.getUIElements()) {
            if (!(elemObj instanceof ModLabeledToggleButton)) {
                continue;
            }
            ModLabeledToggleButton button = (ModLabeledToggleButton) elemObj;
            String text = button.text == null ? null : button.text.text;
            if (text == null) {
                continue;
            }
            String zh = null;
            if (KEYWORD_ICONS_EN.equals(text)) {
                zh = KEYWORD_ICONS_ZH;
            } else if (MASK_EN.equals(text)) {
                zh = MASK_ZH;
            } else if (UNLOCK_EN.equals(text)) {
                zh = UNLOCK_ZH;
            }
            if (zh != null) {
                button.text.text = zh;
                button.toggle.wrapHitboxToText(zh, 1000.0f, 0.0f, button.text.font);
                ++changed;
            }
        }
        logger.info("Translated " + changed + " settings panel buttons");
    }
}
