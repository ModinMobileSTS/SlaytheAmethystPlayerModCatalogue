package legendarypotionszhspatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import legendarypotionsmod.potions.LegendaryPotionPool;

/** Generated from AgentPatchTargetInspection(targetClassName=legendarypotionsmod.relics.common.WitchsTrunk, memberName=onEquip, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/legendarypotions.jar, overloads=[AgentPatchMethodSignature(name=onEquip, descriptor=()V, parameterTypes=[], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=NAME, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=ID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=RARITY, type=com.megacrit.cardcrawl.relics.AbstractRelic$RelicTier, isStatic=true), AgentPatchFieldSignature(name=SOUND, type=com.megacrit.cardcrawl.relics.AbstractRelic$LandingSound, isStatic=true)]).targetClassName#onEquip ()V. */
@SpirePatch2(
    cls = "legendarypotionsmod.relics.common.WitchsTrunk",
    method = "onEquip",
    paramtypez = {}
)
public final class LoadoutWitchsTrunkPatch {
    private LoadoutWitchsTrunkPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(legendarypotionsmod.relics.common.WitchsTrunk __instance) {
        if (!LoadoutWitchRelics.consume(__instance)) {
            return SpireReturn.Continue();
        }
        if (AbstractDungeon.player != null) {
            LegendaryPotionPool.loadPool();
            AbstractPotion potion = LegendaryPotionPool.getRandomLegendaryPotion(
                AbstractDungeon.relicRng, false).makeCopy();
            AbstractDungeon.player.obtainPotion(potion);
        }
        return SpireReturn.Return();
    }
}
