package natsu.translation;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

@SpirePatch2(cls = "natsu.NatsuCard", method = "canUse", paramtypez = {com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class})
public final class ChineseCantUsePatch {
    private ChineseCantUsePatch() { }

    @SpirePostfixPatch
    public static boolean Postfix(boolean __result, natsu.NatsuCard __instance, Object[] __args) {
        if (ChineseText.enabled() && !__result && __instance.cantUseMessage != null && __instance.cantUseMessage.startsWith("I need ")) {
            __instance.cantUseMessage = "需要 " + __instance.stat("pay") + " 层怀旧才能回忆。";
        }
        return __result;
    }
}
