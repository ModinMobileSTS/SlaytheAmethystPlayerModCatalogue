package legendarypotionszhspatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/** Generated from AgentPatchTargetInspection(targetClassName=com.megacrit.cardcrawl.rooms.AbstractRoom, memberName=spawnRelicAndObtain, classOrigin=/storage/emulated/0/Android/data/io.stamethyst/files/sts/desktop-1.0.jar, overloads=[AgentPatchMethodSignature(name=spawnRelicAndObtain, descriptor=(FFLcom/megacrit/cardcrawl/relics/AbstractRelic;)V, parameterTypes=[float, float, com.megacrit.cardcrawl.relics.AbstractRelic], returnType=void, isStatic=false, hasCode=true, localVariables=[], bytecode=null, bytecodeTruncated=false)], fields=[AgentPatchFieldSignature(name=uiStrings, type=com.megacrit.cardcrawl.localization.UIStrings, isStatic=true), AgentPatchFieldSignature(name=TEXT, type=java.lang.String[], isStatic=true), AgentPatchFieldSignature(name=logger, type=org.apache.logging.log4j.Logger, isStatic=true), AgentPatchFieldSignature(name=potions, type=java.util.ArrayList, isStatic=false), AgentPatchFieldSignature(name=relics, type=java.util.ArrayList, isStatic=false), AgentPatchFieldSignature(name=rewards, type=java.util.ArrayList, isStatic=false), AgentPatchFieldSignature(name=souls, type=com.megacrit.cardcrawl.cards.SoulGroup, isStatic=false), AgentPatchFieldSignature(name=phase, type=com.megacrit.cardcrawl.rooms.AbstractRoom$RoomPhase, isStatic=false), AgentPatchFieldSignature(name=event, type=com.megacrit.cardcrawl.events.AbstractEvent, isStatic=false), AgentPatchFieldSignature(name=monsters, type=com.megacrit.cardcrawl.monsters.MonsterGroup, isStatic=false), AgentPatchFieldSignature(name=endBattleTimer, type=float, isStatic=false), AgentPatchFieldSignature(name=rewardPopOutTimer, type=float, isStatic=false), AgentPatchFieldSignature(name=END_TURN_WAIT_DURATION, type=float, isStatic=true), AgentPatchFieldSignature(name=mapSymbol, type=java.lang.String, isStatic=false), AgentPatchFieldSignature(name=mapImg, type=com.badlogic.gdx.graphics.Texture, isStatic=false), AgentPatchFieldSignature(name=mapImgOutline, type=com.badlogic.gdx.graphics.Texture, isStatic=false), AgentPatchFieldSignature(name=isBattleOver, type=boolean, isStatic=false), AgentPatchFieldSignature(name=cannotLose, type=boolean, isStatic=false), AgentPatchFieldSignature(name=eliteTrigger, type=boolean, isStatic=false), AgentPatchFieldSignature(name=blizzardPotionMod, type=int, isStatic=true), AgentPatchFieldSignature(name=BLIZZARD_POTION_MOD_AMT, type=int, isStatic=true), AgentPatchFieldSignature(name=mugged, type=boolean, isStatic=false), AgentPatchFieldSignature(name=smoked, type=boolean, isStatic=false), AgentPatchFieldSignature(name=combatEvent, type=boolean, isStatic=false), AgentPatchFieldSignature(name=rewardAllowed, type=boolean, isStatic=false), AgentPatchFieldSignature(name=rewardTime, type=boolean, isStatic=false), AgentPatchFieldSignature(name=skipMonsterTurn, type=boolean, isStatic=false), AgentPatchFieldSignature(name=baseRareCardChance, type=int, isStatic=false), AgentPatchFieldSignature(name=baseUncommonCardChance, type=int, isStatic=false), AgentPatchFieldSignature(name=rareCardChance, type=int, isStatic=false), AgentPatchFieldSignature(name=uncommonCardChance, type=int, isStatic=false), AgentPatchFieldSignature(name=waitTimer, type=float, isStatic=true)]).targetClassName#spawnRelicAndObtain (FFLcom/megacrit/cardcrawl/relics/AbstractRelic;)V. */
@SpirePatch2(
    cls = "com.megacrit.cardcrawl.rooms.AbstractRoom",
    method = "spawnRelicAndObtain",
    paramtypez = { float.class, float.class, com.megacrit.cardcrawl.relics.AbstractRelic.class }
)
public final class LoadoutWitchRelicTrackingPatch {
    private LoadoutWitchRelicTrackingPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(com.megacrit.cardcrawl.rooms.AbstractRoom __instance, Object[] __args) {
        if (__args.length >= 3 && __args[2] instanceof com.megacrit.cardcrawl.relics.AbstractRelic) {
            LoadoutWitchRelics.markIfLoadout((com.megacrit.cardcrawl.relics.AbstractRelic)__args[2]);
        }
    }
}
