package com.navsysfix.patches;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.dsaita.NavSys.NavSysMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.navsysfix.Reflect;

/**
 * NavSys creates its ShapeRenderer lazily and never disposes it, so every act
 * (and every run) leaks a GL object. receiveStartAct is the natural place to
 * release it: the renderer is recreated on the next map render.
 */
@SpirePatch2(clz = NavSysMod.class, method = "receiveStartAct", paramtypez = {})
public class PatchReceiveStartAct {

    @SpirePostfixPatch
    public static void Postfix(NavSysMod __instance) {
        ShapeRenderer renderer = Reflect.shapeRenderer(__instance);
        if (renderer == null) {
            return;
        }
        try {
            renderer.dispose();
        } catch (Throwable ignored) {
        }
        Reflect.setShapeRenderer(__instance, null);
    }
}
