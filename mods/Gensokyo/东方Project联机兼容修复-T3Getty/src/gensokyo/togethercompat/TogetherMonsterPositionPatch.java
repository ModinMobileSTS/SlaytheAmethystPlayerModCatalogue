package gensokyo.togethercompat;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

import java.lang.reflect.Field;

/**
 * Keeps monster positions stable when Gensokyo is played with Together in Spire.
 *
 * Together serialises every combat monster as a NetworkMonster. To rebuild it on
 * another client it stores constructor offsets computed with its own
 * SpireVariables.scale (which defaults to 1). The game, however, originally
 * placed the monster with Settings.xScale/yScale. On screens where those scales
 * are not 1 (4:3, letterboxed or ultrawide, common on mobile) the two scales
 * differ, so every monster rebuilt from sync data is shifted. This patch only
 * touches monsters reconstructed by Together, only during a connected session,
 * and only when the two scales actually differ. Single player is unaffected.
 */
public final class TogetherMonsterPositionPatch {
    private static final String NETWORK_MONSTER_CLASS =
            "spireTogether.network.objects.entities.NetworkMonster";
    private static final String TOGETHER_SCALE_CLASS = "spireTogether.util.SpireVariables";

    private TogetherMonsterPositionPatch() {
    }

    @SpirePatch2(cls = "com.megacrit.cardcrawl.monsters.AbstractMonster", method = "<ctor>",
            paramtypez = {String.class, String.class, int.class, float.class, float.class,
                    float.class, float.class, String.class, float.class, float.class, boolean.class})
    public static class CorrectSyncedMonsterPosition {
        @SpirePostfixPatch
        public static void Postfix(AbstractMonster __instance) {
            if (__instance == null || !MultiplayerGuard.isMultiplayer()) {
                return;
            }
            float[] togetherScale = getTogetherScale();
            float scaleX = togetherScale[0];
            float scaleY = togetherScale[1];
            if (scaleX == 0.0f || scaleY == 0.0f
                    || Settings.xScale == 0.0f || Settings.yScale == 0.0f) {
                return;
            }
            if (Math.abs(scaleX - Settings.xScale) < 0.0001f
                    && Math.abs(scaleY - Settings.yScale) < 0.0001f) {
                return;
            }
            if (!builtByTogether()) {
                return;
            }
            correct(__instance, scaleX, scaleY);
        }
    }

    private static void correct(AbstractMonster monster, float scaleX, float scaleY) {
        float anchorX = (float) Settings.WIDTH * 0.75f;
        float anchorY = AbstractDungeon.floorY;
        float targetX = anchorX + (monster.drawX - anchorX) * scaleX / Settings.xScale;
        float targetY = anchorY + (monster.drawY - anchorY) * scaleY / Settings.yScale;
        float deltaX = targetX - monster.drawX;
        float deltaY = targetY - monster.drawY;
        if (Float.isNaN(deltaX) || Float.isNaN(deltaY)
                || (deltaX == 0.0f && deltaY == 0.0f)) {
            return;
        }
        monster.drawX = targetX;
        monster.drawY = targetY;
        shift(monster.hb, deltaX, deltaY);
        shift(monster.healthHb, deltaX, deltaY);
        shift(monster.intentHb, deltaX, deltaY);
    }

    private static void shift(Hitbox hb, float dx, float dy) {
        if (hb == null) {
            return;
        }
        hb.x += dx;
        hb.y += dy;
        hb.cX += dx;
        hb.cY += dy;
    }

    private static boolean builtByTogether() {
        try {
            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                if (element != null && NETWORK_MONSTER_CLASS.equals(element.getClassName())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Reads Together's scale vector reflectively; defaults to (1, 1) as in the mod. */
    private static float[] getTogetherScale() {
        try {
            Class<?> variables = Class.forName(TOGETHER_SCALE_CLASS);
            Field scale = variables.getField("scale");
            Object vector = scale.get(null);
            if (vector == null) {
                return new float[]{1.0f, 1.0f};
            }
            Field x = vector.getClass().getField("x");
            Field y = vector.getClass().getField("y");
            return new float[]{x.getFloat(vector), y.getFloat(vector)};
        } catch (Throwable ignored) {
            return new float[]{1.0f, 1.0f};
        }
    }
}
