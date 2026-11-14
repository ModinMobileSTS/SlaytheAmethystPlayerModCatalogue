package cardforge.zh;

import com.megacrit.cardcrawl.core.Settings;

/**
 * Card Forge 汉化所需的文本常量。
 *
 * <p>原 mod 把遗物名称存放在 localization/RelicStrings.json 里，描述与选卡提示则硬编码在三个遗物类中，
 * 这里统一存放中英两套文本，供各处补丁按当前游戏语言取用。</p>
 */
public final class Texts {
    public static final String SEETHING_FIRE_ID = "Seething Fire";
    public static final String HAMMER_OF_THOR_ID = "Hammer of Thor";
    public static final String ANVIL_OF_THE_GODS_ID = "Anvil of the Gods";

    private static final String SEETHING_FIRE_NAME_EN = "Seething Fire";
    private static final String SEETHING_FIRE_FLAVOR_EN = "A hot smoldering fire that can destroy any cards";
    private static final String SEETHING_FIRE_DESC_EN = "Right-clicking this relic allows you to destroy any cards";

    private static final String HAMMER_OF_THOR_NAME_EN = "Hammer of Thor";
    private static final String HAMMER_OF_THOR_FLAVOR_EN = "The hammer of Thor himself, capable of crafting any cards";
    private static final String HAMMER_OF_THOR_DESC_EN = "Right-clicking this relic allows you craft any cards";

    private static final String ANVIL_OF_THE_GODS_NAME_EN = "Anvil of the Gods";
    private static final String ANVIL_OF_THE_GODS_FLAVOR_EN = "A great anvil capable of upgrading any cards";
    private static final String ANVIL_OF_THE_GODS_DESC_EN = "Right-clicking this relic allows you upgrading any cards";

    private static final String SEETHING_FIRE_NAME_ZH = "灼热之火";
    private static final String SEETHING_FIRE_FLAVOR_ZH = "一团炽热闷燃的火焰，能够烧毁任何卡牌。";
    static final String SEETHING_FIRE_DESC_ZH = "长按或右键点击此遗物，可以摧毁任意数量的卡牌。";
    static final String SEETHING_FIRE_GRID_ZH = "选择任意数量的卡牌摧毁";

    private static final String HAMMER_OF_THOR_NAME_ZH = "雷神之锤";
    private static final String HAMMER_OF_THOR_FLAVOR_ZH = "雷神索尔本人的战锤，可以铸造任何卡牌。";
    static final String HAMMER_OF_THOR_DESC_ZH = "长按或右键点击此遗物，可以铸造任意数量的卡牌。";
    static final String HAMMER_OF_THOR_GRID_ZH = "选择任意数量的卡牌铸造";

    private static final String ANVIL_OF_THE_GODS_NAME_ZH = "众神之砧";
    private static final String ANVIL_OF_THE_GODS_FLAVOR_ZH = "一尊伟大的铁砧，可以升级任何卡牌。";
    static final String ANVIL_OF_THE_GODS_DESC_ZH = "长按或右键点击此遗物，可以升级任意数量的卡牌。";
    static final String ANVIL_OF_THE_GODS_GRID_ZH = "选择任意数量的卡牌升级";

    private Texts() {
    }

    /** 当前游戏语言是否为中文（设置尚未初始化时按中文处理）。 */
    public static boolean useChinese() {
        Settings.GameLanguage language = Settings.language;
        return language == null
                || language == Settings.GameLanguage.ZHS
                || language == Settings.GameLanguage.ZHT;
    }

    /** 按当前语言取中/英文本。 */
    public static String t(String zh, String en) {
        return useChinese() ? zh : en;
    }

    /** 遗物名；不属于 Card Forge 的 id 返回 null。 */
    public static String name(String relicId) {
        boolean zh = useChinese();
        if (SEETHING_FIRE_ID.equals(relicId)) {
            return zh ? SEETHING_FIRE_NAME_ZH : SEETHING_FIRE_NAME_EN;
        }
        if (HAMMER_OF_THOR_ID.equals(relicId)) {
            return zh ? HAMMER_OF_THOR_NAME_ZH : HAMMER_OF_THOR_NAME_EN;
        }
        if (ANVIL_OF_THE_GODS_ID.equals(relicId)) {
            return zh ? ANVIL_OF_THE_GODS_NAME_ZH : ANVIL_OF_THE_GODS_NAME_EN;
        }
        return null;
    }

    /** 遗物风味文本；不属于 Card Forge 的 id 返回 null。 */
    public static String flavor(String relicId) {
        boolean zh = useChinese();
        if (SEETHING_FIRE_ID.equals(relicId)) {
            return zh ? SEETHING_FIRE_FLAVOR_ZH : SEETHING_FIRE_FLAVOR_EN;
        }
        if (HAMMER_OF_THOR_ID.equals(relicId)) {
            return zh ? HAMMER_OF_THOR_FLAVOR_ZH : HAMMER_OF_THOR_FLAVOR_EN;
        }
        if (ANVIL_OF_THE_GODS_ID.equals(relicId)) {
            return zh ? ANVIL_OF_THE_GODS_FLAVOR_ZH : ANVIL_OF_THE_GODS_FLAVOR_EN;
        }
        return null;
    }

    /** 遗物描述；不属于 Card Forge 的 id 返回 null。 */
    public static String description(String relicId) {
        boolean zh = useChinese();
        if (SEETHING_FIRE_ID.equals(relicId)) {
            return zh ? SEETHING_FIRE_DESC_ZH : SEETHING_FIRE_DESC_EN;
        }
        if (HAMMER_OF_THOR_ID.equals(relicId)) {
            return zh ? HAMMER_OF_THOR_DESC_ZH : HAMMER_OF_THOR_DESC_EN;
        }
        if (ANVIL_OF_THE_GODS_ID.equals(relicId)) {
            return zh ? ANVIL_OF_THE_GODS_DESC_ZH : ANVIL_OF_THE_GODS_DESC_EN;
        }
        return null;
    }
}
