package chronopatch.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** Generated from AgentPatchTargetInspection(targetClassName=chronomuncher.powers.HastePower, memberName=updateDescription, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/TheDisciple.jar, overloads=[AgentPatchMethodSignature(name=updateDescription, descriptor=()V, parameterTypes=[], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=POWER_ID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=powerStrings, type=com.megacrit.cardcrawl.localization.PowerStrings, isStatic=true), AgentPatchFieldSignature(name=NAME, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=DESCRIPTIONS, type=java.lang.String[], isStatic=true)]).targetClassName#updateDescription ()V. */
@SpirePatch2(
    cls = "chronomuncher.powers.HastePower",
    method = "updateDescription",
    paramtypez = {}
)
public final class FixHastePowerDesc {
    private FixHastePowerDesc() {
    }

    @SpirePostfixPatch
    public static void Postfix(chronomuncher.powers.HastePower __instance) {
        // TODO: implement the requested behavior. __args contains target arguments by position.
    }
}
