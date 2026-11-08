package customrelicszh;

import com.megacrit.cardcrawl.core.Settings;

/**
 * 简体中文本地化文本的唯一来源。
 * 遗物名、风味文本、描述同时用于 RelicStrings 注册与 getUpdatedDescription 覆盖，
 * 因此只需要在这里维护一份中文文本。
 */
public final class ZhText {

    public static final String BAG_OF_GREED_ID = "BagOfGreed";
    public static final String HOURGLASS_OF_TIME_ID = "HourglassOfTime";
    public static final String LUCKY_DICE_ID = "LuckyDice";
    public static final String PHOENIX_FEATHER_ID = "PhoenixFeather";
    public static final String TALISMAN_OF_VENGEANCE_ID = "TalismanOfVengeance";

    public static final String BAG_OF_GREED_NAME = "贪婪之袋";
    public static final String BAG_OF_GREED_FLAVOR = "一个神秘的袋子，似乎总能把钱币吸引到主人身边。";
    public static final String BAG_OF_GREED_DESC = "每当你击败一名非小兵敌人时，获得 #b8 点额外金币。";

    public static final String HOURGLASS_OF_TIME_NAME = "时光沙漏";
    public static final String HOURGLASS_OF_TIME_FLAVOR = "流沙无尽地流逝，既带来力量，也带来衰败。";
    public static final String HOURGLASS_OF_TIME_DESC = "每回合开始时，获得 [E]。 NL 每回合结束时，失去 #b1 点生命。";

    public static final String LUCKY_DICE_NAME = "幸运骰子";
    public static final String LUCKY_DICE_FLAVOR = "幸运眷顾勇敢者——你觉得自己运气如何？";
    public static final String LUCKY_DICE_DESC = "每场战斗开始时，随机获得 #b1 点力量、#b1 点敏捷，或 #b2 层再生。";

    public static final String PHOENIX_FEATHER_NAME = "凤凰之羽";
    public static final String PHOENIX_FEATHER_FLAVOR = "一片绚丽的羽毛，至今仍散发着浴火重生的温暖。";
    public static final String PHOENIX_FEATHER_DESC = "每场战斗开始时，若你的生命值低于 #b50%，回复 #b25% 的最大生命值。";

    public static final String TALISMAN_OF_VENGEANCE_NAME = "复仇护符";
    public static final String TALISMAN_OF_VENGEANCE_FLAVOR = "一枚古老的护符，会向伤害其持有者的人施以报复。";
    public static final String TALISMAN_OF_VENGEANCE_DESC = "每当你因敌人的攻击失去生命时，获得 #b3 点格挡。";

    private ZhText() {
    }

    /** 仅在游戏语言为简体中文时生效，其它语言保持原模组的英文文本。 */
    public static boolean isChinese() {
        return Settings.language == Settings.GameLanguage.ZHS;
    }

    private static String entry(String name, String flavor, String desc) {
        return "{\"NAME\": \"" + name + "\",\"FLAVOR\": \"" + flavor
                + "\",\"DESCRIPTIONS\": [\"" + desc + "\"]}";
    }

    /** 与父模组使用同一组遗物 ID，因此会覆盖其英文 RelicStrings。 */
    public static String relicStringsJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append('"').append(BAG_OF_GREED_ID).append("\": ")
                .append(entry(BAG_OF_GREED_NAME, BAG_OF_GREED_FLAVOR, BAG_OF_GREED_DESC)).append(',');
        sb.append('"').append(HOURGLASS_OF_TIME_ID).append("\": ")
                .append(entry(HOURGLASS_OF_TIME_NAME, HOURGLASS_OF_TIME_FLAVOR, HOURGLASS_OF_TIME_DESC)).append(',');
        sb.append('"').append(LUCKY_DICE_ID).append("\": ")
                .append(entry(LUCKY_DICE_NAME, LUCKY_DICE_FLAVOR, LUCKY_DICE_DESC)).append(',');
        sb.append('"').append(PHOENIX_FEATHER_ID).append("\": ")
                .append(entry(PHOENIX_FEATHER_NAME, PHOENIX_FEATHER_FLAVOR, PHOENIX_FEATHER_DESC)).append(',');
        sb.append('"').append(TALISMAN_OF_VENGEANCE_ID).append("\": ")
                .append(entry(TALISMAN_OF_VENGEANCE_NAME, TALISMAN_OF_VENGEANCE_FLAVOR,
                        TALISMAN_OF_VENGEANCE_DESC));
        sb.append('}');
        return sb.toString();
    }
}
