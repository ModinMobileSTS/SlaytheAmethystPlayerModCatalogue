package therailgunfix;

import TheRailgun.cards.Iron_Dust_Vial;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = Iron_Dust_Vial.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class IronDustVialDrawFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractDungeon.actionManager.addToTop(new DrawCardAction((AbstractPlayer) source, 1));
        return SpireReturn.Return();
    }
}
