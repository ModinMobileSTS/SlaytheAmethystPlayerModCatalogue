package downfallvfxfix;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Helper used by the Slimebound orb patches.
 *
 * Slimebound orbs (SpawnedSlime subclasses) create their particle objects lazily
 * inside SpawnedSlime#render/postSpawnEffects, but their cleanUpVFX() methods call
 * finish() on those fields without checking for null. If an orb is evoked or removed
 * before its first rendered frame (channel overflow at battle start, room / battle
 * state sync in co-op and so on) the game dies with a NullPointerException.
 *
 * This helper only reads/finishes the effect objects that really exist, so the
 * clean up step becomes a no-op instead of a crash.
 */
public final class VfxGuard {

    private VfxGuard() {
    }

    public static Field field(Class<?> type, String name) {
        try {
            Field f = type.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Throwable t) {
            return null;
        }
    }

    public static Object get(Field field, Object instance) {
        if (field == null || instance == null) {
            return null;
        }
        try {
            return field.get(instance);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Calls finish() on the given effect object when it exists; never throws. */
    public static void finish(Object effect) {
        if (effect == null) {
            return;
        }
        try {
            Method m = effect.getClass().getMethod("finish");
            m.setAccessible(true);
            m.invoke(effect);
        } catch (Throwable ignored) {
            // Cleaning up visuals must never break the game.
        }
    }
}
