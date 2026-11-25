package nobmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.utility.UseCardAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.powers.AngerPower;
import spireTogether.SpireTogetherMod;

@SpirePatch2(clz = AngerPower.class, method = "onUseCard", paramtypez = {AbstractCard.class, UseCardAction.class})
public final class LocalAngerPatch {
    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(AngerPower __instance, Object[] __args) {
        if (!SpireTogetherMod.isConnected || !(__instance.owner instanceof GremlinNob)) {
            return SpireReturn.Continue();
        }
        if (TeamAnger.isSkillPlay((AbstractCard) __args[0]) && TeamAnger.isActive(__instance)) {
            TeamAnger.count(__instance, TeamAnger.localQuota());
        }
        return SpireReturn.Return();
    }
}
