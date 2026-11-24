package therailgunfix;

import TheRailgun.cards.Gekotamania;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = Gekotamania.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class GekotamaniaDrawFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Gekotamania __instance, Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int perRelic = __instance.upgraded ? 2 : 1;
        AbstractDungeon.actionManager.addToTop(new DrawCardAction(p, p.relics.size() * perRelic));
        return SpireReturn.Return();
    }
}
