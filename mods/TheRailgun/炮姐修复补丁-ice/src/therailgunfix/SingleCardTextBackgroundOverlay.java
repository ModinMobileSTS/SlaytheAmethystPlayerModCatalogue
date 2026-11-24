package therailgunfix;

import basemod.ReflectionHacks;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.screens.SingleCardViewPopup;

/** Same black description panel in the single-card inspection view. */
@SpirePatch2(
    clz = com.megacrit.cardcrawl.screens.SingleCardViewPopup.class,
    method = "renderCardBack",
    paramtypez = { SpriteBatch.class }
)
public final class SingleCardTextBackgroundOverlay {
    private SingleCardTextBackgroundOverlay() {
    }

    @SpirePostfixPatch
    public static void Postfix(SingleCardViewPopup __instance, Object[] __args) {
        SpriteBatch sb = (SpriteBatch) __args[0];
        AbstractCard card = (AbstractCard) ReflectionHacks.getPrivate(__instance, SingleCardViewPopup.class, "card");
        if (!RailgunCards.isRailgunCard(card)) {
            return;
        }
        float scale = Settings.scale;
        RailgunCards.drawPanel(sb, Settings.WIDTH / 2f, Settings.HEIGHT / 2f,
                528f * scale, 784f * scale, 0f);
    }
}
