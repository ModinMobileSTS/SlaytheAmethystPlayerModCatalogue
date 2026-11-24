package therailgunfix;

import TheRailgun.cards.Heroinism;
import TheRailgun.powers.HeroinismPower;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.IntangiblePower;

@SpirePatch2(clz = Heroinism.class, method = "use", paramtypez = {AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class})
public class HeroinismCardFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new HeroinismPower(p, p, 1), 1));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new IntangiblePower(p, 1), 1));
        return SpireReturn.Return();
    }
}
