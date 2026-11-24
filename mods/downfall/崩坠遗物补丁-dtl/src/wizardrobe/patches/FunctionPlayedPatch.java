package wizardrobe.patches;

import automaton.cards.FunctionCard;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import wizardrobe.relics.PastLifeMemory;

/** 未实际使用的自动打出清理动作不会经过这个入口。 */
@SpirePatch2(clz = AbstractPlayer.class, method = "useCard",
        paramtypez = {AbstractCard.class, AbstractMonster.class, int.class})
public final class FunctionPlayedPatch {
    private FunctionPlayedPatch() {}

    @SpirePostfixPatch
    public static void Postfix(AbstractPlayer __instance, Object[] __args) {
        AbstractCard c = (AbstractCard) __args[0];
        if (!(c instanceof FunctionCard) || c.dontTriggerOnUseCard
                || __instance != AbstractDungeon.player || __instance.cardInUse != c) {
            return;
        }
        AbstractRelic relic = __instance.getRelic(PastLifeMemory.ID);
        if (relic instanceof PastLifeMemory) {
            ((PastLifeMemory) relic).onFunctionPlayed();
        }
    }
}
