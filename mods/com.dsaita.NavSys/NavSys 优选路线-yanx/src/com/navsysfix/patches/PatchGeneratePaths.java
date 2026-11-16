package com.navsysfix.patches;

import com.dsaita.NavSys.NavSysMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/**
 * NavSys.generatePathsIfNeeded() calls currMapNode.equals(...) without a null
 * check, while PathGenerator.generateAllPossiblePaths() explicitly guards for a
 * null AbstractDungeon.getCurrMapNode(). The null case is real, and an NPE here
 * aborts the whole receiveRender pass, taking the panel and the highlighting
 * down with it. Skip the refresh instead.
 */
@SpirePatch2(clz = NavSysMod.class, method = "generatePathsIfNeeded", paramtypez = {})
public class PatchGeneratePaths {

    @SpirePrefixPatch
    public static SpireReturn Prefix(NavSysMod __instance) {
        if (AbstractDungeon.getCurrMapNode() == null) {
            return SpireReturn.Return(false);
        }
        return SpireReturn.Continue();
    }
}
