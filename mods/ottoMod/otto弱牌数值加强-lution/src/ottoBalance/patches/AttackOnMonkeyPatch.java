package ottoBalance.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** Generated from AgentPatchTargetInspection(targetClassName=ottoMod.cards.AttackOnMonkey, memberName=<ctor>, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/ottoMod.jar, overloads=[AgentPatchMethodSignature(name=<ctor>, descriptor=()V, parameterTypes=[], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=ID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=cardStrings, type=com.megacrit.cardcrawl.localization.CardStrings, isStatic=true), AgentPatchFieldSignature(name=NAME, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=DESCRIPTION, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=UPGRADE_DESCRIPTION, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=IMG_PATH, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=COST, type=int, isStatic=true)]).targetClassName#<ctor> ()V. */
@SpirePatch2(
    cls = "ottoMod.cards.AttackOnMonkey",
    method = "<ctor>",
    paramtypez = {}
)
public final class AttackOnMonkeyPatch {
    private AttackOnMonkeyPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        // TODO: implement the requested behavior. __args contains target arguments by position.
    }
}
