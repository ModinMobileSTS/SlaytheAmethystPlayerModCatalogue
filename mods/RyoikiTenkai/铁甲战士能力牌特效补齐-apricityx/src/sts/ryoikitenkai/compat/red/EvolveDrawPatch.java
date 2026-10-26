package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.powers.EvolvePower;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.powers.EvolvePower",
    method = "onCardDraw",
    paramtypez = { com.megacrit.cardcrawl.cards.AbstractCard.class }
)
public final class EvolveDrawPatch {
    private EvolveDrawPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(EvolvePower __instance, Object[] __args) {
        AbstractCard card = (AbstractCard) __args[0];
        if (card == null || card.type != AbstractCard.CardType.STATUS) {
            return;
        }
        if (__instance.owner != null && __instance.owner.hasPower("No Draw")) {
            return;
        }
        RedEffectRegistry.drawEvolve(__instance.owner);
    }
}
