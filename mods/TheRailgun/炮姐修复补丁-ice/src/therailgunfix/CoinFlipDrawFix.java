package therailgunfix;

import TheRailgun.cards.Coin_Flip;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.DrawCardAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.Lightning;

@SpirePatch2(clz = Coin_Flip.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class CoinFlipDrawFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        AbstractDungeon.actionManager.addToBottom(new ChannelAction(new Lightning()));
        AbstractDungeon.actionManager.addToBottom(new DrawCardAction(p, 1));
        return SpireReturn.Return();
    }
}
