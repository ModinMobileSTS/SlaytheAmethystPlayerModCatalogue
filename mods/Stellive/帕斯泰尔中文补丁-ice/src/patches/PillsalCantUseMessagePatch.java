package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import code.cards.YuzuhaRiko.Ri_12_Pillsal;

@SpirePatch2(
    cls = "code.cards.YuzuhaRiko.Ri_12_Pillsal",
    method = "canUse",
    paramtypez = {AbstractPlayer.class, AbstractMonster.class}
)
public final class PillsalCantUseMessagePatch {
    @SpirePostfixPatch
    public static boolean Postfix(boolean __result, Ri_12_Pillsal __instance, Object[] __args) {
        CantUseMessageTranslation.translate(__instance);
        return __result;
    }
}
