package estaliafix;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;

/**
 * 保持原版 F6 异画提示（位置、字体、颜色与原版完全一致）。
 * 原版绘制逻辑在补丁应用阶段被中和成空实现，这里重新用正确签名实现。
 */
@SpirePatch(clz = SingleCardViewPopup.class, method = "render", paramtypez = {SpriteBatch.class})
public class ArtHintPatch {

    @SpirePostfixPatch
    public static void after(SingleCardViewPopup __instance, SpriteBatch sb) {
        AbstractCard shown = (AbstractCard) ReflectionHacks.getPrivate(__instance,
                SingleCardViewPopup.class, "card");
        if (!(shown instanceof estalia.Card)) {
            return;
        }
        if (((estalia.Card) shown).spec.hidden) {
            return;
        }
        FontHelper.renderFontLeftTopAligned(sb, FontHelper.tipBodyFont,
                "F6  切换此牌异画",
                52.0f * Settings.scale,
                (float) Settings.HEIGHT - 66.0f * Settings.scale,
                Color.WHITE);
    }
}
