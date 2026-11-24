package challengethespire.pool;

import basemod.BaseMod;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.core.Settings;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Boss selection for the "Modded Boss Rush" challenge.
 *
 * <p>The parent mod only asks BaseMod for bosses registered under the four vanilla dungeon IDs.
 * Mods built on Act Like It! register their bosses under their own act IDs, so those encounters
 * were never drawn. Bosses are now grouped back into the act they belong to (via BaseMod's act
 * registry and Act Like It!'s act table), and a boss can be chosen only once per run.</p>
 *
 * <p>The whole four-stage assignment is recomputed from the run seed on every call, so save/load
 * regenerating the map produces the same encounters.</p>
 */
public final class CustomActBossPool {

    private static final String GENERATE_MAP_HOOK =
            "challengeTheSpire.patches.com.megacrit.cardcrawl.dungeons.AbstractDungeon.GenerateMapHook";
    private static final String CHALLENGE_CLASS = "challengeTheSpire.ChallengeTheSpire";

    private static final String[] ACT_ORDER = {"Exordium", "TheCity", "TheBeyond", "TheEnding"};
    private static final int[] ACT_COUNTS = {3, 3, 3, 1};

    private static final Map<Integer, String> SLOT_TO_ACT = new HashMap<Integer, String>();
    static {
        SLOT_TO_ACT.put(Integer.valueOf(1), "Exordium");
        SLOT_TO_ACT.put(Integer.valueOf(2), "TheCity");
        SLOT_TO_ACT.put(Integer.valueOf(3), "TheBeyond");
        SLOT_TO_ACT.put(Integer.valueOf(4), "TheEnding");
    }

    private CustomActBossPool() {
    }

    @SpirePatch2(cls = GENERATE_MAP_HOOK, method = "getModdedBosses",
            paramtypez = {String.class, int.class, Random.class})
    public static class IncludeAllRegisteredBosses {
        @SpirePrefixPatch
        public static SpireReturn<List> before(Object[] __args) {
            String dungeonID = (String) __args[0];
            int numBosses = ((Integer) __args[1]).intValue();

            List<String> chosen = assignAllBosses(dungeonID, numBosses).get(dungeonID);
            if (chosen == null) {
                chosen = new ArrayList<String>();
            }
            BaseMod.logger.info("Bosses for " + dungeonID + " (act-grouped):");
            for (String id : chosen) {
                BaseMod.logger.info("  " + id);
            }
            return SpireReturn.Return(chosen);
        }
    }

    /** Builds the complete four-stage assignment so no encounter can be drawn twice. */
    private static Map<String, List<String>> assignAllBosses(String currentAct, int currentCount) {
        Map<String, Set<String>> grouped = new LinkedHashMap<String, Set<String>>();
        for (String act : ACT_ORDER) {
            grouped.put(act, new LinkedHashSet<String>());
        }

        Map<String, Integer> registeredActs = registeredActNumbers();
        Map<?, ?> actLikeItActs = actLikeItActNumbers();
        Map<?, ?> allBosses = (Map<?, ?>) ReflectionHacks.getPrivateStatic(BaseMod.class, "customBosses");
        if (allBosses != null) {
            for (Object key : new ArrayList<Object>(allBosses.keySet())) {
                if (!(key instanceof String)) {
                    continue;
                }
                String act = actFor((String) key, registeredActs, actLikeItActs);
                if (act == null) {
                    BaseMod.logger.debug("Skipping boss IDs of unclassified act: " + key);
                    continue;
                }
                List<String> ids = BaseMod.getBossIDs((String) key);
                if (ids != null) {
                    grouped.get(act).addAll(ids);
                }
            }
        }

        Random rng = new Random(Settings.seed.longValue());
        Set<String> used = new HashSet<String>();
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        for (int i = 0; i < ACT_ORDER.length; i++) {
            String act = ACT_ORDER[i];
            int count = act.equals(currentAct) ? currentCount : ACT_COUNTS[i];

            List<String> modded = new ArrayList<String>(grouped.get(act));
            modded.removeAll(used);
            Collections.shuffle(modded, rng);

            List<String> chosen = new ArrayList<String>(count);
            for (String id : modded) {
                if (chosen.size() >= count) {
                    break;
                }
                chosen.add(id);
            }
            used.addAll(chosen);

            if (chosen.size() < count) {
                List<String> vanilla = new ArrayList<String>(vanillaBosses(act));
                vanilla.removeAll(used);
                Collections.shuffle(vanilla, rng);
                for (String id : vanilla) {
                    if (chosen.size() >= count) {
                        break;
                    }
                    chosen.add(id);
                    used.add(id);
                }
            }
            result.put(act, chosen);
        }
        return result;
    }

    /** Maps any registered dungeon ID (vanilla, Act Like It! custom act, etc.) to a Rush stage. */
    private static String actFor(String dungeonID, Map<String, Integer> registeredActs,
                                 Map<?, ?> actLikeItActs) {
        for (String act : ACT_ORDER) {
            if (act.equals(dungeonID)) {
                return act;
            }
        }
        Integer slot = registeredActs.get(dungeonID);
        if (slot != null && SLOT_TO_ACT.containsKey(slot)) {
            return SLOT_TO_ACT.get(slot);
        }
        if (actLikeItActs != null) {
            for (Map.Entry<?, ?> entry : actLikeItActs.entrySet()) {
                if (!SLOT_TO_ACT.containsKey(entry.getKey()) || !(entry.getValue() instanceof List)) {
                    continue;
                }
                if (((List<?>) entry.getValue()).contains(dungeonID)) {
                    return SLOT_TO_ACT.get(entry.getKey());
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Integer> registeredActNumbers() {
        try {
            Class<?> actCommand = Class.forName("basemod.devcommands.act.ActCommand");
            Map<String, Integer> acts =
                    (Map<String, Integer>) ReflectionHacks.getPrivateStatic(actCommand, "acts");
            if (acts != null) {
                return acts;
            }
        } catch (Exception ignored) {
            // No act registry available; fall back to Act Like It!'s own table.
        }
        return new HashMap<String, Integer>();
    }

    private static Map<?, ?> actLikeItActNumbers() {
        try {
            Class<?> customDungeon = Class.forName("actlikeit.dungeons.CustomDungeon");
            Field field = customDungeon.getField("actnumbers");
            return (Map<?, ?>) field.get(null);
        } catch (Exception ignored) {
            return null;
        }
    }

    /** Use the parent mod's list (kept canonical by ChallengeRushFixes), with a local fallback. */
    @SuppressWarnings("unchecked")
    private static List<String> vanillaBosses(String act) {
        try {
            Class<?> parent = Class.forName(CHALLENGE_CLASS);
            Field field = parent.getField("bossIDs");
            Map<?, ?> bosses = (Map<?, ?>) field.get(null);
            if (bosses != null && bosses.get(act) instanceof List) {
                return new ArrayList<String>((List<String>) bosses.get(act));
            }
        } catch (Exception ignored) {
            // Fall through to the hardcoded list below.
        }
        if ("Exordium".equals(act)) {
            return new ArrayList<String>(Arrays.asList("The Guardian", "Hexaghost", "Slime Boss"));
        }
        if ("TheCity".equals(act)) {
            return new ArrayList<String>(Arrays.asList("Automaton", "Collector", "Champ"));
        }
        if ("TheBeyond".equals(act)) {
            return new ArrayList<String>(Arrays.asList("Awakened One", "Time Eater", "Donu and Deca"));
        }
        if ("TheEnding".equals(act)) {
            return new ArrayList<String>(Arrays.asList("The Heart"));
        }
        return new ArrayList<String>();
    }
}
