package gensokyo.togethercompat;

import com.evacipated.cardcrawl.modthespire.Loader;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.lang.reflect.Field;

/**
 * Detects an active Together in Spire (spireTogether) multiplayer session.
 *
 * Reflection is used deliberately: Gensokyo does not declare spireTogether as a
 * dependency, so this patch must not have a hard compile/runtime reference to it.
 */
public final class MultiplayerGuard {
    private static final String TOGETHER_MOD_ID = "spireTogether";
    private static Boolean togetherLoaded;

    private MultiplayerGuard() {
    }

    public static boolean isTogetherLoaded() {
        if (togetherLoaded == null) {
            try {
                togetherLoaded = Loader.isModLoaded(TOGETHER_MOD_ID);
            } catch (Throwable t) {
                togetherLoaded = Boolean.FALSE;
            }
        }
        return togetherLoaded;
    }

    public static boolean isMultiplayer() {
        if (!isTogetherLoaded()) {
            return false;
        }
        try {
            Class<?> modClass = Class.forName("spireTogether.SpireTogetherMod");
            Field connected = modClass.getField("isConnected");
            return connected.getBoolean(null);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Gensokyo's spell-card (Position) and minion hooks read AbstractDungeon.player
     * and the current room's monster group directly. Together in Spire runs some of
     * these hooks while a remote player is being synchronized, when one of those
     * objects may not exist yet. Single player always returns false here so the
     * original behaviour is untouched.
     */
    public static boolean hasUnsafeCombatContext() {
        if (!isMultiplayer()) {
            return false;
        }
        try {
            if (AbstractDungeon.player == null || AbstractDungeon.getCurrRoom() == null
                    || AbstractDungeon.getCurrRoom().monsters == null) {
                return true;
            }
        } catch (Throwable t) {
            return true;
        }
        return false;
    }
}
