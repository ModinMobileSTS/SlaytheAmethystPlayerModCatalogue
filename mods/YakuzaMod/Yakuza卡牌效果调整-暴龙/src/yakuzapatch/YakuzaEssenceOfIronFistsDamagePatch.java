package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import theYakuza.cards.RareAttacks.YakuzaEssenceOfIronFists;

@SpirePatch2(
    cls = "theYakuza.cards.RareAttacks.YakuzaEssenceOfIronFists",
    method = "<ctor>",
    paramtypez = {}
)
public final class YakuzaEssenceOfIronFistsDamagePatch {
    private YakuzaEssenceOfIronFistsDamagePatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(YakuzaEssenceOfIronFists __instance) {
        __instance.baseDamage = 14;
        __instance.damage = 14;
    }
}
