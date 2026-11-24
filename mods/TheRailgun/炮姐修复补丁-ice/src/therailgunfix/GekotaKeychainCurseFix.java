package therailgunfix;

import TheRailgun.cards.Gekota_Keychain;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ExhaustSpecificCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import java.util.ArrayList;
import java.util.List;

@SpirePatch2(clz = Gekota_Keychain.class, method = "use", paramtypez = {AbstractPlayer.class, AbstractMonster.class})
public class GekotaKeychainCurseFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Gekota_Keychain __instance, Object[] __args) {
        Object source = __args[0];
        if (!(source instanceof AbstractPlayer)) {
            return SpireReturn.Continue();
        }
        AbstractPlayer p = (AbstractPlayer) source;
        int needed = __instance.upgraded ? 2 : 1;
        int removed = 0;
        removed += exhaustCurses(p, p.hand.group, needed - removed);
        removed += exhaustCurses(p, p.drawPile.group, needed - removed);
        removed += exhaustCurses(p, p.discardPile.group, needed - removed);
        return SpireReturn.Return();
    }

    private static int exhaustCurses(AbstractPlayer p, List<AbstractCard> group, int limit) {
        int removed = 0;
        for (AbstractCard c : new ArrayList<>(group)) {
            if (removed >= limit) {
                break;
            }
            if (c != null && c.type == AbstractCard.CardType.CURSE) {
                if (group == p.hand.group) {
                    AbstractDungeon.actionManager.addToTop(new ExhaustSpecificCardAction(c, p.hand));
                } else if (group == p.drawPile.group) {
                    AbstractDungeon.actionManager.addToTop(new ExhaustSpecificCardAction(c, p.drawPile));
                } else {
                    AbstractDungeon.actionManager.addToTop(new ExhaustSpecificCardAction(c, p.discardPile));
                }
                ++removed;
            }
        }
        return removed;
    }
}
