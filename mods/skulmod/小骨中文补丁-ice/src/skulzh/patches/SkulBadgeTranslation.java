package skulzh.patches;

import basemod.ModBadge;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import skulzh.SkulChinese;

/** Generated from AgentPatchTargetInspection(targetClassName=basemod.ModBadge, memberName=<ctor>, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/required_mods/BaseMod.jar, overloads=[AgentPatchMethodSignature(name=<ctor>, descriptor=(Lcom/badlogic/gdx/graphics/Texture;FFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lbasemod/ModPanel;)V, parameterTypes=[com.badlogic.gdx.graphics.Texture, float, float, java.lang.String, java.lang.String, java.lang.String, basemod.ModPanel], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=logger, type=org.apache.logging.log4j.Logger, isStatic=true), AgentPatchFieldSignature(name=texture, type=com.badlogic.gdx.graphics.Texture, isStatic=false), AgentPatchFieldSignature(name=modName, type=java.lang.String, isStatic=false), AgentPatchFieldSignature(name=tip, type=java.lang.String, isStatic=false), AgentPatchFieldSignature(name=x, type=float, isStatic=false), AgentPatchFieldSignature(name=y, type=float, isStatic=false), AgentPatchFieldSignature(name=w, type=float, isStatic=false), AgentPatchFieldSignature(name=h, type=float, isStatic=false), AgentPatchFieldSignature(name=hb, type=com.megacrit.cardcrawl.helpers.Hitbox, isStatic=false), AgentPatchFieldSignature(name=modPanel, type=basemod.ModPanel, isStatic=false)]).targetClassName#<ctor> (Lcom/badlogic/gdx/graphics/Texture;FFLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Lbasemod/ModPanel;)V. */
@SpirePatch2(
    cls = "basemod.ModBadge",
    method = "<ctor>",
    paramtypez = { com.badlogic.gdx.graphics.Texture.class, float.class, float.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, basemod.ModPanel.class }
)
public final class SkulBadgeTranslation {
    private SkulBadgeTranslation() {
    }

    @SpirePostfixPatch
    public static void Postfix(ModBadge __instance, Object[] __args) {
        if (!SkulChinese.enabled()) return;

        String name = (String) __args[3];
        String desc = (String) __args[5];
        Object settingsPanel = __args[6];

        if ("小骨".equals(name) && desc != null && desc.startsWith("Reassemble, rise")) {
            ReflectionHacks.setPrivate(__instance, ModBadge.class, "modName", "小骨");
            ReflectionHacks.setPrivate(__instance, ModBadge.class, "tip",
                "Svitasch NL 重新组装，站起来，再次站起来！借助阵亡战士留下的头骨引导他们的力量。");
        } else if ("BaseMod".equals(name) && settingsPanel != null
            && "Modding API and Dev console.".equals(desc)) {
            ReflectionHacks.setPrivate(__instance, ModBadge.class, "modName", "小骨设置");
            ReflectionHacks.setPrivate(__instance, ModBadge.class, "tip",
                "Skul 中文汉化 NL 点击打开小骨模组设置。");
        }
    }
}
