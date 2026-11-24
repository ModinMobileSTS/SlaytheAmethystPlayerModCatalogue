package therailgunfix;

import TheRailgun.cards.Supercharge;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.DiscardAction;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = Supercharge.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class SuperchargeUseFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Supercharge __instance, Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int draw = __instance.upgraded ? 4 : 3;
        int discard = __instance.upgraded ? 2 : 1;
        AbstractDungeon.actionManager.addToBottom(new DrawCardAction(p, draw));
        AbstractDungeon.actionManager.addToBottom(new DiscardAction(p, p, discard, false));
        return SpireReturn.Return();
    }
}
