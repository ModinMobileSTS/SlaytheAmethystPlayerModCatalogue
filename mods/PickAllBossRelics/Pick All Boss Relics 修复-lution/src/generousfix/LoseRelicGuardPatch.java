package generousfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * The parent mod's skip/cancel button (BossRelicSelectScreen.relicSkipLogic) calls
 * loseRelic() on every relic taken in the current selection. Since those relics were
 * already obtained (their onEquip side effects - card purge, upgrades, rewards - are
 * irreversible), taking them back left the run inconsistent. Relics claimed during the
 * current boss selection are protected here instead.
 */
@SpirePatch2(clz = AbstractPlayer.class, method = "loseRelic", paramtypez = {String.class})
public class LoseRelicGuardPatch {

    @SpirePrefixPatch
    public static SpireReturn<Boolean> Prefix(AbstractPlayer __instance, Object[] __args) {
        if (__args != null && __args.length > 0 && __args[0] instanceof String) {
            if (FixSupport.consumeClaim((String) __args[0])) {
                return SpireReturn.Return(false);
            }
        }
        return SpireReturn.Continue();
    }
}
