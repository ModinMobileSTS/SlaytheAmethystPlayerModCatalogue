package acustartrelics;

import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 配置持久化（SpireConfig）与可选遗物清单。
 * 清单取 RelicLibrary 的各稀有度列表去重合并，因此其它 mod 加入共享池的遗物也会出现在面板里。
 */
public final class StartRelicSettings {

    private static final Logger logger = LogManager.getLogger("StartRelicSettings");

    private static final String CONFIG_ID = "AllCardsUpgradedExt";
    private static final String CONFIG_FILE = "startRelics";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_SELECTED = "relicIds";

    private static SpireConfig config;
    private static boolean enabled = true;
    private static final LinkedHashSet<String> selectedIds = new LinkedHashSet<String>();
    private static ArrayList<AbstractRelic> catalog;

    private StartRelicSettings() {
    }

    public static void init() {
        try {
            config = new SpireConfig(CONFIG_ID, CONFIG_FILE);
            if (config.has(KEY_ENABLED)) {
                enabled = config.getBool(KEY_ENABLED);
            }
            selectedIds.clear();
            if (config.has(KEY_SELECTED)) {
                String raw = config.getString(KEY_SELECTED);
                if (raw != null) {
                    for (String part : raw.split(",")) {
                        String id = part.trim();
                        if (!id.isEmpty()) {
                            selectedIds.add(id);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load start relic config", e);
        }
        getCatalog();
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static boolean isSelected(String relicId) {
        return relicId != null && selectedIds.contains(relicId);
    }

    public static void toggle(String relicId) {
        if (relicId == null) {
            return;
        }
        if (!selectedIds.remove(relicId)) {
            selectedIds.add(relicId);
        }
        save();
    }

    public static void clearSelection() {
        selectedIds.clear();
        save();
    }

    public static int getSelectedCount() {
        return selectedIds.size();
    }

    public static List<String> getSelectedIds() {
        return new ArrayList<String>(selectedIds);
    }

    public static ArrayList<AbstractRelic> getCatalog() {
        if (catalog == null) {
            catalog = buildCatalog();
        }
        return catalog;
    }

    private static ArrayList<AbstractRelic> buildCatalog() {
        LinkedHashMap<String, AbstractRelic> byId = new LinkedHashMap<String, AbstractRelic>();
        collect(byId, RelicLibrary.starterList);
        collect(byId, RelicLibrary.commonList);
        collect(byId, RelicLibrary.uncommonList);
        collect(byId, RelicLibrary.rareList);
        collect(byId, RelicLibrary.shopList);
        collect(byId, RelicLibrary.bossList);
        collect(byId, RelicLibrary.specialList);

        ArrayList<AbstractRelic> result = new ArrayList<AbstractRelic>(byId.values());
        Collections.sort(result, new Comparator<AbstractRelic>() {
            @Override
            public int compare(AbstractRelic a, AbstractRelic b) {
                int ta = a.tier == null ? 99 : a.tier.ordinal();
                int tb = b.tier == null ? 99 : b.tier.ordinal();
                if (ta != tb) {
                    return ta < tb ? -1 : 1;
                }
                String na = a.name == null ? "" : a.name;
                String nb = b.name == null ? "" : b.name;
                return na.compareToIgnoreCase(nb);
            }
        });
        logger.info("Start relic catalog built: " + result.size() + " relics");
        return result;
    }

    private static void collect(LinkedHashMap<String, AbstractRelic> target, List<AbstractRelic> source) {
        if (source == null) {
            return;
        }
        for (AbstractRelic relic : source) {
            if (relic == null || relic.relicId == null || relic.relicId.isEmpty()) {
                continue;
            }
            if (!target.containsKey(relic.relicId)) {
                target.put(relic.relicId, relic);
            }
        }
    }

    private static void save() {
        if (config == null) {
            return;
        }
        try {
            config.setBool(KEY_ENABLED, enabled);
            config.setString(KEY_SELECTED, String.join(",", selectedIds));
            config.save();
        } catch (Exception e) {
            logger.warn("Failed to save start relic config", e);
        }
    }
}
