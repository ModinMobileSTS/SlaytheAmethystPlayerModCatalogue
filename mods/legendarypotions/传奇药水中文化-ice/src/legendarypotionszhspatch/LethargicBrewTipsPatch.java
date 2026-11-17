package legendarypotionszhspatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.PowerTip;

/** Generated from AgentPatchTargetInspection(targetClassName=legendarypotionsmod.potions.LethargicBrew, memberName=addAdditionalTips, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/legendarypotions.jar, overloads=[AgentPatchMethodSignature(name=addAdditionalTips, descriptor=()V, parameterTypes=[], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=ID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=LIQUID_COLOR, type=com.badlogic.gdx.graphics.Color, isStatic=true), AgentPatchFieldSignature(name=HYBRID_COLOR, type=com.badlogic.gdx.graphics.Color, isStatic=true), AgentPatchFieldSignature(name=SPOTS_COLOR, type=com.badlogic.gdx.graphics.Color, isStatic=true)]).targetClassName#addAdditionalTips ()V. */
@SpirePatch2(
    cls = "legendarypotionsmod.potions.LethargicBrew",
    method = "addAdditionalTips",
    paramtypez = {}
)
public final class LethargicBrewTipsPatch {
    private LethargicBrewTipsPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(legendarypotionsmod.potions.LethargicBrew __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        for (int i = __instance.tips.size() - 1; i >= 0; --i) {
            PowerTip tip = __instance.tips.get(i);
            if ("Slow".equals(tip.header)) {
                __instance.tips.remove(i);
                break;
            }
        }
        __instance.tips.add(new PowerTip(
            "缓慢",
            "当你打出一张牌时，敌人在本回合受到的#y攻击伤害增加#b10%。"
        ));
    }
}
