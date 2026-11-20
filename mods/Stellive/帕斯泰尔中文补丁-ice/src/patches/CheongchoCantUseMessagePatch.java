package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import code.powers.YuzuhaRiko.Ri_CheongchoPower;

@SpirePatch2(
    cls = "code.powers.YuzuhaRiko.Ri_CheongchoPower",
    method = "canPlayCard",
    paramtypez = {AbstractCard.class}
)
public final class CheongchoCantUseMessagePatch {
    @SpirePostfixPatch
    public static boolean Postfix(boolean __result, Ri_CheongchoPower __instance, Object[] __args) {
        CantUseMessageTranslation.translate((AbstractCard) __args[0]);
        return __result;
    }
}
