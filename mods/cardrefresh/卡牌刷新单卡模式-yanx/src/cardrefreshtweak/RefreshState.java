package cardrefreshtweak;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.core.Settings;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Per-run refresh bookkeeping.
 *
 * A small persistent snapshot is kept per run seed:
 *  - charges:        whole-group rerolls left (the original mod's counter),
 *  - slotUsed:       single-card rerolls already used, indexed by card position,
 *  - grantFloor / grantRemaining:
 *                    floor and unclaimed card reward count of the reward that was handled last.
 *
 * Recognising a reward: the card reward item is only removed from the room reward list when the
 * player takes or skips it, and that list is rebuilt from the save in the same order, so a reward
 * that was open when the game was saved reports the same floor and the same number of unclaimed
 * card rewards again. That keeps a reward from paying out twice when the screen is opened again.
 *
 * Loading a save: with {@code RefreshConfig.refundOnReload} on (default) the card reward that is
 * open again is handled exactly like the first version of this patch did, i.e. as a fresh reward,
 * so the refreshes spent on it are available again (whole-group counter +1, single-card counters
 * refilled). Turning the setting off keeps the spent counters instead.
 */
public final class RefreshState {

    private static final Logger LOG = LogManager.getLogger("CardRefreshTweak");

    private static final String MOD_ID = "cardrefresh";
    private static final String CONFIG_NAME = "refresh_state";
    private static final String LEGACY_CONFIG_NAME = "rerolls";

    private static final String KEY_CHARGES = "charges.";
    private static final String KEY_GRANT_FLOOR = "grantfloor.";
    private static final String KEY_GRANT_REMAINING = "grantremaining.";
    private static final String KEY_SLOT_USED = "slotused.";
    /** Written by an earlier build; only read to migrate mid reward saves. */
    private static final String KEY_LEGACY_OPEN = "rewardopen.";

    public static final int CHARGE_PER_REWARD = 1;
    public static final int MAX_CHARGES = 99;

    /** Whole-group rerolls available (the original mod's counter). */
    public static int charges = 0;
    /** Seed of the run the current values belong to. */
    public static String runKey = null;
    /** Floor of the card reward that was handled last. */
    public static int grantFloor = -1;
    /** Unclaimed card rewards in the room then, -1 when unknown. */
    public static int grantRemaining = -1;
    /** Single-card rerolls already used, indexed by card position in the reward. */
    public static int[] slotUsed = new int[0];

    /** "floor:remaining" pairs already handled in this session, cleared on load and new run. */
    private static final Set<String> sessionRewards = new HashSet<String>();
    /** Set when the stored state comes from an earlier build while a reward was open. */
    private static boolean legacyOpenPending = false;
    /** Set right after a run save was restored, consumed by the first reward that is opened. */
    private static boolean refundPending = false;

    private static SpireConfig config = null;
    private static boolean configTried = false;
    private static SpireConfig legacyConfig = null;
    private static boolean legacyTried = false;

    private RefreshState() {
    }

    public static String currentSeed() {
        Long seed = Settings.seed;
        return seed == null ? null : seed.toString();
    }

    /** Loads the stored numbers whenever the run seed changed (new run, or after loading a save). */
    public static void ensureRun() {
        String seed = currentSeed();
        if (seed == null || seed.equals(runKey)) {
            return;
        }
        runKey = seed;
        sessionRewards.clear();
        load(seed);
        LOG.info("[CardRefresh] run " + seed + " charges=" + charges
                + " floor=" + grantFloor + " remaining=" + grantRemaining
                + " slotUsed=" + joinInts(slotUsed));
    }

    /**
     * Called after a run save was restored: the per session bookkeeping is dropped and the card
     * reward that is open again may pay out once more, like the first version of this patch did.
     */
    public static void onRunLoaded() {
        sessionRewards.clear();
        legacyOpenPending = false;
        runKey = null;
        ensureRun();
        refundPending = true;
        LOG.info("[CardRefresh] run loaded, refreshes of the reopened reward are given back");
    }

    public static void resetRun() {
        runKey = currentSeed();
        sessionRewards.clear();
        legacyOpenPending = false;
        refundPending = false;
        charges = 0;
        grantFloor = -1;
        grantRemaining = -1;
        slotUsed = new int[0];
        save();
    }

    private static void load(String seed) {
        charges = 0;
        grantFloor = -1;
        grantRemaining = -1;
        slotUsed = new int[0];
        legacyOpenPending = false;
        SpireConfig cfg = config();
        boolean found = false;
        if (cfg != null) {
            try {
                if (cfg.has(KEY_CHARGES + seed)) {
                    charges = clampCharges(cfg.getInt(KEY_CHARGES + seed));
                    found = true;
                }
                if (cfg.has(KEY_GRANT_FLOOR + seed)) {
                    grantFloor = cfg.getInt(KEY_GRANT_FLOOR + seed);
                }
                if (cfg.has(KEY_GRANT_REMAINING + seed)) {
                    grantRemaining = cfg.getInt(KEY_GRANT_REMAINING + seed);
                } else if (cfg.has(KEY_LEGACY_OPEN + seed) && cfg.getBool(KEY_LEGACY_OPEN + seed)) {
                    // Written by an earlier build: a reward that already paid out was still open
                    // when the game stopped, so it may be the same reward when it is reopened.
                    legacyOpenPending = true;
                }
                if (cfg.has(KEY_SLOT_USED + seed)) {
                    slotUsed = parseInts(cfg.getString(KEY_SLOT_USED + seed));
                }
            } catch (Throwable t) {
                LOG.warn("[CardRefresh] cannot read stored refresh state: " + t);
            }
        }
        if (!found) {
            int legacy = legacyCharges(seed);
            if (legacy > 0) {
                charges = clampCharges(legacy);
                LOG.info("[CardRefresh] imported " + charges + " rerolls from the previous version");
            }
        }
    }

    public static void save() {
        String seed = runKey;
        if (seed == null) {
            return;
        }
        SpireConfig cfg = config();
        if (cfg == null) {
            return;
        }
        try {
            cfg.setInt(KEY_CHARGES + seed, clampCharges(charges));
            cfg.setInt(KEY_GRANT_FLOOR + seed, grantFloor);
            cfg.setInt(KEY_GRANT_REMAINING + seed, grantRemaining);
            cfg.setString(KEY_SLOT_USED + seed, joinInts(slotUsed));
            cfg.setBool(KEY_LEGACY_OPEN + seed, false);
            cfg.save();
        } catch (Throwable t) {
            LOG.warn("[CardRefresh] cannot store refresh state: " + t);
        }
    }

    /**
     * Handles the card reward that is being opened right now.
     *
     * @param floor     current dungeon floor
     * @param remaining unclaimed card rewards in the room, including this one (-1 if unknown)
     * @param size      number of card options in this reward
     * @return true when this reward had already paid out and nothing was changed
     */
    public static boolean beginReward(int floor, int remaining, int size) {
        boolean seenThisSession = sessionRewards.contains(floor + ":" + remaining);
        sessionRewards.add(floor + ":" + remaining);

        boolean same = !seenThisSession && floor == grantFloor;
        if (same && remaining >= 0 && grantRemaining >= 0) {
            same = remaining == grantRemaining;
        }
        if (!same && legacyOpenPending && floor == grantFloor) {
            same = true;
        }
        legacyOpenPending = false;

        if (same && refundPending && RefreshConfig.refundOnReload) {
            // Loaded save: hand the refreshes of this reward back, like the first version did.
            charges = clampCharges(charges + CHARGE_PER_REWARD);
            slotUsed = new int[Math.max(0, size)];
            same = false;
            LOG.info("[CardRefresh] reloaded reward, refreshes given back: charges=" + charges
                    + " slot=" + joinInts(slotUsed));
        } else if (same) {
            resizeSlots(size);
            LOG.info("[CardRefresh] reward reopened, counters kept: charges=" + charges
                    + " slot=" + joinInts(slotUsed));
        } else {
            charges = clampCharges(charges + CHARGE_PER_REWARD);
            slotUsed = new int[Math.max(0, size)];
            LOG.info("[CardRefresh] new reward: charges=" + charges
                    + " slot=" + joinInts(slotUsed));
        }
        refundPending = false;
        grantFloor = floor;
        grantRemaining = remaining;
        save();
        return same;
    }

    public static int usedFor(int index) {
        if (index < 0 || index >= slotUsed.length) {
            return 0;
        }
        return slotUsed[index];
    }

    public static void markUsed(int index) {
        if (index < 0) {
            return;
        }
        if (index >= slotUsed.length) {
            slotUsed = resize(slotUsed, index + 1);
        }
        slotUsed[index] = Math.max(0, slotUsed[index]) + 1;
    }

    public static void resizeSlots(int size) {
        slotUsed = resize(slotUsed, Math.max(0, size));
    }

    private static int[] resize(int[] src, int size) {
        int[] out = new int[size];
        if (src != null) {
            System.arraycopy(src, 0, out, 0, Math.min(src.length, size));
        }
        return out;
    }

    private static int clampCharges(int value) {
        return Math.max(0, Math.min(MAX_CHARGES, value));
    }

    private static SpireConfig config() {
        if (!configTried) {
            configTried = true;
            try {
                config = new SpireConfig(MOD_ID, CONFIG_NAME);
            } catch (Throwable t) {
                LOG.warn("[CardRefresh] cannot open config " + CONFIG_NAME + ": " + t);
                config = null;
            }
        }
        return config;
    }

    private static int legacyCharges(String seed) {
        if (!legacyTried) {
            legacyTried = true;
            try {
                legacyConfig = new SpireConfig(MOD_ID, LEGACY_CONFIG_NAME);
            } catch (Throwable t) {
                legacyConfig = null;
            }
        }
        if (legacyConfig == null) {
            return 0;
        }
        try {
            return legacyConfig.has(KEY_CHARGES + seed) ? legacyConfig.getInt(KEY_CHARGES + seed) : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String joinInts(int[] values) {
        if (values == null || values.length == 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(Math.max(0, values[i]));
        }
        return builder.toString();
    }

    private static int[] parseInts(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new int[0];
        }
        String[] parts = value.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; ++i) {
            try {
                out[i] = Math.max(0, Integer.parseInt(parts[i].trim()));
            } catch (Throwable t) {
                out[i] = 0;
            }
        }
        return out;
    }
}
