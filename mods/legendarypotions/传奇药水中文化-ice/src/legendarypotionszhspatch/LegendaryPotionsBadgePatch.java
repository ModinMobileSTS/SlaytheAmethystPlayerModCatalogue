package legendarypotionszhspatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.core.Settings;

/** Generated from AgentPatchTargetInspection(targetClassName=legendarypotionsmod.legendarypotions, memberName=receivePostInitialize, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/legendarypotions.jar, overloads=[AgentPatchMethodSignature(name=receivePostInitialize, descriptor=()V, parameterTypes=[], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=info, type=com.evacipated.cardcrawl.modthespire.ModInfo, isStatic=true), AgentPatchFieldSignature(name=modID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=resourcesFolder, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=logger, type=org.apache.logging.log4j.Logger, isStatic=true), AgentPatchFieldSignature(name=defaultLanguage, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=keywords, type=java.util.Map, isStatic=true)]).targetClassName#receivePostInitialize ()V. */
@SpirePatch2(
    cls = "legendarypotionsmod.legendarypotions",
    method = "receivePostInitialize",
    paramtypez = {}
)
public final class LegendaryPotionsBadgePatch {
    private LegendaryPotionsBadgePatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(legendarypotionsmod.legendarypotions __instance) {
        if (Settings.language == Settings.GameLanguage.ZHS
                && legendarypotionsmod.legendarypotions.info != null) {
            legendarypotionsmod.legendarypotions.info.Name = "传奇药水";
            legendarypotionsmod.legendarypotions.info.Description =
                "添加6瓶强力的传奇药水。它们不会在普通战斗奖励或商店中出现，只能通过6件新增遗物获得。"
                + "此外还增加3件提供通用药水支持的遗物。";
        }
    }
}
