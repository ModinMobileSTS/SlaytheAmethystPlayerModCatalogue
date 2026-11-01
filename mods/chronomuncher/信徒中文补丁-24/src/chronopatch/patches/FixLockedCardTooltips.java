    package chronopatch.patches;

    import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
    import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

    /** Generated from AgentPatchTargetInspection(targetClassName=chronomuncher.cards.LockedUrn, memberName=getCustomTooltips, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/mods_library/TheDisciple.jar, overloads=[AgentPatchMethodSignature(name=getCustomTooltips, descriptor=()Ljava/util/List;, parameterTypes=[], returnType=java.util.List, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=ID, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=cardStrings, type=com.megacrit.cardcrawl.localization.CardStrings, isStatic=true), AgentPatchFieldSignature(name=NAME, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=DESCRIPTION, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=UPGRADE_DESCRIPTION, type=java.lang.String, isStatic=true), AgentPatchFieldSignature(name=EXTENDED_DESCRIPTION, type=java.lang.String[], isStatic=true), AgentPatchFieldSignature(name=COST, type=int, isStatic=true), AgentPatchFieldSignature(name=tips, type=java.util.ArrayList, isStatic=false)]).targetClassName#getCustomTooltips ()Ljava/util/List;. */
    @SpirePatch2(
        cls = "chronomuncher.cards.LockedUrn",
        method = "getCustomTooltips",
        paramtypez = {}
    )
    public final class FixLockedCardTooltips {
        private FixLockedCardTooltips() {
        }

        @SpirePostfixPatch
        public static java.util.List Postfix(java.util.List __result, chronomuncher.cards.LockedUrn __instance) {
            // TODO: implement the requested behavior. __args contains target arguments by position.
return __result;
        }
    }
