package therailgunfix;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ImageMaster;

/** Draws an opaque black panel under the description of every TheRailgun card. */
@SpirePatch2(
    clz = com.megacrit.cardcrawl.cards.AbstractCard.class,
    method = "renderCardBg",
    paramtypez = { SpriteBatch.class, float.class, float.class }
)
public final class CardTextBackgroundOverlay {
    private CardTextBackgroundOverlay() {
    }

    @SpirePostfixPatch
    public static void Postfix(AbstractCard __instance, Object[] __args) {
        SpriteBatch sb = (SpriteBatch) __args[0];
        float x = ((Float) __args[1]).floatValue();
        float y = ((Float) __args[2]).floatValue();
        if (!RailgunCards.isRailgunCard(__instance)) {
            return;
        }
        float scale = __instance.drawScale * Settings.scale;
        // Fill the whole area inside the decorative frame of the 512px card
        // background; the frame itself lies outside this rectangle.
        RailgunCards.drawPanel(sb, x, y, 264f * scale, 392f * scale, __instance.angle);
    }
}
