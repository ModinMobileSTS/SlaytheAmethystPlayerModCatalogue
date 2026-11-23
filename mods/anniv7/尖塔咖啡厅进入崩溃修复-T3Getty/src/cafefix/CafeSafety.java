package cafefix;

import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spireCafe.Anniv7Mod;
import spireCafe.abstracts.AbstractAttraction;
import spireCafe.abstracts.AbstractBartender;
import spireCafe.abstracts.AbstractCafeInteractable;
import spireCafe.abstracts.AbstractMerchant;
import spireCafe.abstracts.AbstractPatron;
import spireCafe.interactables.patrons.spiomesmanifestation.SpiomesManifestationPatron;

/**
 * Shared, side-effect free helpers used by the Spire Cafe entry hardening patches.
 * Everything here is defensive: failures are logged and turned into a safe fallback
 * instead of being rethrown, because these paths run while the cafe room is entered.
 */
public final class CafeSafety {

    public static final Logger LOG = LogManager.getLogger("SpireCafeEntryFix");

    private static final Map<Class<?>, Method> CAN_SPAWN_METHODS = new HashMap<Class<?>, Method>();
    private static final Set<Class<?>> CAN_SPAWN_MISSING = new HashSet<Class<?>>();

    private CafeSafety() {
    }

    /** The per-run "already seen" set can be null on unusual load orders; never dereference it blindly. */
    public static void ensureSeenInteractables() {
        try {
            if (Anniv7Mod.currentRunSeenInteractables == null) {
                LOG.warn("[CafeFix] currentRunSeenInteractables was null while entering the cafe; rebuilding it.");
                Anniv7Mod.currentRunSeenInteractables = new HashSet<String>();
            }
        } catch (Throwable t) {
            LOG.warn("[CafeFix] Could not initialise currentRunSeenInteractables", t);
        }
    }

    /**
     * Replacement for CafeRoom.getPossibilities: every registered interactable of the requested
     * type that passes the filters. Never throws, so an exploding canSpawn() cannot abort room entry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Class<? extends AbstractCafeInteractable>> pick(Class<? extends AbstractCafeInteractable> base,
                                                                      boolean respectSeen,
                                                                      boolean respectCanSpawn) {
        List<Class<? extends AbstractCafeInteractable>> result = new ArrayList<Class<? extends AbstractCafeInteractable>>();
        if (base == null) {
            return result;
        }
        HashMap classes = Anniv7Mod.interactableClasses;
        if (classes == null) {
            return result;
        }
        Set seen = Anniv7Mod.currentRunSeenInteractables;
        for (Object o : classes.entrySet()) {
            if (!(o instanceof Map.Entry)) {
                continue;
            }
            Map.Entry entry = (Map.Entry) o;
            Object value = entry.getValue();
            if (!(value instanceof Class)) {
                continue;
            }
            Class<?> candidate = (Class<?>) value;
            if (!base.isAssignableFrom(candidate)) {
                continue;
            }
            if (respectSeen && seen != null && seen.contains(entry.getKey())) {
                continue;
            }
            if (respectCanSpawn && !safeCanSpawn(candidate)) {
                continue;
            }
            result.add((Class<? extends AbstractCafeInteractable>) candidate);
        }
        return result;
    }

    /** Reflection based canSpawn() lookup that treats a throwing hook as "cannot spawn" instead of crashing. */
    public static boolean safeCanSpawn(Class<?> clz) {
        if (clz == null) {
            return false;
        }
        Method method = canSpawnMethod(clz);
        if (method == null) {
            return true;
        }
        try {
            Object value = method.invoke(null);
            return !(value instanceof Boolean) || ((Boolean) value).booleanValue();
        } catch (Throwable t) {
            LOG.warn("[CafeFix] canSpawn() of " + clz.getName() + " threw; treating it as unavailable for this run.", t);
            return false;
        }
    }

    private static Method canSpawnMethod(Class<?> clz) {
        if (CAN_SPAWN_MISSING.contains(clz)) {
            return null;
        }
        Method cached = CAN_SPAWN_METHODS.get(clz);
        if (cached != null) {
            return cached;
        }
        try {
            for (Method m : clz.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                if (!"canSpawn".equals(m.getName())) {
                    continue;
                }
                if (!Boolean.TYPE.equals(m.getReturnType())) {
                    continue;
                }
                if (m.getParameterCount() != 0) {
                    continue;
                }
                try {
                    m.setAccessible(true);
                } catch (Throwable ignored) {
                    // still usable if the declaring class is accessible
                }
                CAN_SPAWN_METHODS.put(clz, m);
                return m;
            }
        } catch (Throwable t) {
            LOG.warn("[CafeFix] Could not scan canSpawn() of " + clz.getName(), t);
        }
        CAN_SPAWN_MISSING.add(clz);
        return null;
    }

    /** Instantiates an interactable through the framework's (float, float) constructor, swallowing failures. */
    public static AbstractCafeInteractable instantiate(Class<?> clz, float x, float y) {
        if (clz == null) {
            return null;
        }
        try {
            Constructor<?> ctor = clz.getConstructor(float.class, float.class);
            return (AbstractCafeInteractable) ctor.newInstance(Float.valueOf(x), Float.valueOf(y));
        } catch (Throwable t) {
            LOG.warn("[CafeFix] Could not create cafe interactable " + clz.getName(), t);
            return null;
        }
    }

    /**
     * Picks a working replacement of the same category when an interactable cannot be constructed,
     * so a single broken resident no longer aborts entering the room.
     */
    public static AbstractCafeInteractable fallback(Class<? extends AbstractCafeInteractable> failed, float x, float y) {
        Class<? extends AbstractCafeInteractable> category = categoryOf(failed);
        if (category == null) {
            return null;
        }
        List<Class<? extends AbstractCafeInteractable>> candidates = pick(category, false, true);
        if (candidates.isEmpty()) {
            candidates = pick(category, false, false);
        }
        for (Class<? extends AbstractCafeInteractable> candidate : candidates) {
            if (candidate == failed) {
                continue;
            }
            AbstractCafeInteractable made = instantiate(candidate, x, y);
            if (made != null) {
                LOG.warn("[CafeFix] Substituted " + candidate.getName() + " for the failing interactable "
                        + (failed == null ? "<null>" : failed.getName()) + ".");
                return made;
            }
        }
        return null;
    }

    private static Class<? extends AbstractCafeInteractable> categoryOf(Class<? extends AbstractCafeInteractable> clz) {
        if (clz == null) {
            return null;
        }
        if (AbstractBartender.class.isAssignableFrom(clz)) {
            return AbstractBartender.class;
        }
        if (AbstractMerchant.class.isAssignableFrom(clz)) {
            return AbstractMerchant.class;
        }
        if (AbstractAttraction.class.isAssignableFrom(clz)) {
            return AbstractAttraction.class;
        }
        if (AbstractPatron.class.isAssignableFrom(clz)) {
            return AbstractPatron.class;
        }
        return null;
    }

    /**
     * Replacement for SpiomesManifestationPatron.rollBiomes. The original ends with
     * possibleBiomes.subList(0, 3), which throws IndexOutOfBoundsException whenever fewer than
     * three biomes are eligible (late acts, most zones already visited), crashing the cafe room entry.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Object> rollBiomes(SpiomesManifestationPatron patron) {
        List<Object> possible = new ArrayList<Object>();
        try {
            Class<?> anniv6 = Class.forName("spireMapOverhaul.SpireAnniversary6Mod");
            Class<?> betterMapGenerator = Class.forName("spireMapOverhaul.BetterMapGenerator");
            Collection allZones = asCollection(readStatic(anniv6, "unfilteredAllZones"));
            Collection activeZones = asCollection(readStatic(betterMapGenerator, "activeZones"));
            Collection currentRunAllZones = asCollection(readStatic(anniv6, "currentRunAllZones"));
            Collection currentRunSeenZones = asCollection(readStatic(anniv6, "currentRunSeenZones"));
            boolean noRepeat = Boolean.TRUE.equals(readStatic(anniv6, "currentRunNoRepeatZones"));
            if (allZones == null) {
                LOG.warn("[CafeFix] Spire Biomes zone list is unavailable; the biomes patron will offer no zones.");
                return possible;
            }
            for (Object biome : allZones) {
                if (biome == null) {
                    continue;
                }
                String id = SpiomesManifestationPatron.getBiomeId(biome);
                if (currentRunAllZones != null && !currentRunAllZones.contains(id)) {
                    continue;
                }
                if (activeZones != null && activeZones.contains(biome)) {
                    continue;
                }
                if (noRepeat && currentRunSeenZones != null && currentRunSeenZones.contains(id)) {
                    continue;
                }
                boolean canSpawn;
                try {
                    canSpawn = patron.getBiomeCanSpawn(biome);
                } catch (Throwable t) {
                    LOG.warn("[CafeFix] Biome " + id + " failed its canSpawn() check; skipping it.", t);
                    canSpawn = false;
                }
                if (!canSpawn) {
                    continue;
                }
                possible.add(biome);
            }
            long seed = AbstractDungeon.miscRng == null ? System.nanoTime() : AbstractDungeon.miscRng.randomLong();
            Collections.shuffle(possible, new Random(seed));
            if (possible.size() > 3) {
                return new ArrayList<Object>(possible.subList(0, 3));
            }
            return possible;
        } catch (Throwable t) {
            LOG.warn("[CafeFix] Failed to roll Spire Biomes options; continuing with an empty list.", t);
            return new ArrayList<Object>();
        }
    }

    /** Invokes the protected abstract AbstractMerchant.rollShop() so the caller can guard it. */
    public static void rollShop(AbstractMerchant merchant) throws Exception {
        Method method = findRollShop(merchant.getClass());
        if (method == null) {
            LOG.warn("[CafeFix] No rollShop() found for " + merchant.getClass().getName());
            return;
        }
        try {
            method.setAccessible(true);
        } catch (Throwable ignored) {
            // invocation may still succeed
        }
        method.invoke(merchant);
    }

    private static Method findRollShop(Class<?> clz) {
        Class<?> current = clz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredMethod("rollShop");
            } catch (NoSuchMethodException e) {
                current = current.getSuperclass();
            } catch (Throwable t) {
                LOG.warn("[CafeFix] Could not inspect rollShop() on " + current.getName(), t);
                return null;
            }
        }
        return null;
    }

    private static Object readStatic(Class<?> clz, String fieldName) {
        try {
            Field field = clz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(null);
        } catch (Throwable t) {
            LOG.warn("[CafeFix] Could not read " + clz.getName() + "." + fieldName, t);
            return null;
        }
    }

    private static Collection<?> asCollection(Object value) {
        return value instanceof Collection ? (Collection<?>) value : null;
    }
}
