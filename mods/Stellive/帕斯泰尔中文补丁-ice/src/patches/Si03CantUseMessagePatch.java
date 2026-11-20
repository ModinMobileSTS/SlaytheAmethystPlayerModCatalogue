package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import code.cards.TenkoShibuki.Si_03_Arrowperm;

@SpirePatch2(
    cls = "code.cards.TenkoShibuki.Si_03_Arrowperm",
    method = "canUse",
    paramtypez = {AbstractPlayer.class, AbstractMonster.class}
)
public final class Si03CantUseMessagePatch {
    @SpirePostfixPatch
    public static boolean Postfix(boolean __result, Si_03_Arrowperm __instance, Object[] __args) {
        CantUseMessageTranslation.translate(__instance);
        return __result;
    }
}
