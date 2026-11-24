package therailgunfix;

import TheRailgun.cards.Railgun;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.actions.common.RemoveAllBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = Railgun.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class RailgunBlockFix {
    @SpirePrefixPatch
    public static void prefix(Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return;
        }
        for (AbstractMonster monster : AbstractDungeon.getMonsters().monsters) {
            if (monster != null) {
                AbstractDungeon.actionManager.addToTop(new RemoveAllBlockAction(monster, (AbstractPlayer) source));
            }
        }
    }
}
