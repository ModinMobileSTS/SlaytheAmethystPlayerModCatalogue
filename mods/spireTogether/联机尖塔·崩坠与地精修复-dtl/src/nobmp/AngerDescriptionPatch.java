package nobmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.powers.AngerPower;
import spireTogether.SpireTogetherMod;

@SpirePatch2(clz = AngerPower.class, method = "updateDescription", paramtypez = {})
public final class AngerDescriptionPatch {
    @SpirePostfixPatch
    public static void Postfix(AngerPower __instance) {
        if (!SpireTogetherMod.isConnected || !(__instance.owner instanceof GremlinNob)) {
            return;
        }
        if (Settings.language == Settings.GameLanguage.ZHS) {
            __instance.description = "全队每打出相当于参战人数的技能牌，获得 #b" + __instance.amount + " 点力量。计数跨回合保留。";
        } else {
            __instance.description = "When the team plays as many Skills as there are players in this fight, gain #b" + __instance.amount + " Strength. The count carries over between turns.";
        }
    }
}
