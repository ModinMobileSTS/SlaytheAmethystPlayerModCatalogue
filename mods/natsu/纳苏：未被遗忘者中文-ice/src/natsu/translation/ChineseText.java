package natsu.translation;

import com.megacrit.cardcrawl.core.Settings;
import java.util.HashMap;
import java.util.Map;

public final class ChineseText {
    private static final Map<String, String> NAMES = new HashMap<String, String>();
    private static final Map<String, String> DESCRIPTIONS = new HashMap<String, String>();

    static {
        put("ALittleCourage", "一点勇气"); put("ANameOfMyOwn", "属于自己的名字");
        put("APlaceBesideYou", "你身边的位置"); put("AbyssalReunion", "深渊重逢");
        put("AllThatRemains", "仅存之物"); put("AnchorOfSelf", "自我之锚");
        put("BleachedPhotograph", "褪色的照片"); put("BorrowedAir", "借来的空气");
        put("BorrowedSummer", "借来的夏日"); put("BottleASigh", "装起一声叹息");
        put("BreatheAgain", "再次呼吸"); put("BrineLash", "盐水之鞭");
        put("BrokenPromise", "破碎的约定"); put("BubbleGarden", "气泡花园");
        put("BubbleNeedle", "气泡针"); put("ChosenFamily", "选择的家人");
        put("ConstellationScars", "星座伤痕"); put("CosmicInk", "宇宙之墨");
        put("DeepExhale", "深深呼气"); put("DeepShelter", "深处的庇护");
        put("DissolvingEdge", "消融之刃"); put("DistantLaughter", "远方的笑声");
        put("Driftwood", "浮木"); put("EchoChamber", "回声室");
        put("EightArmsToHoldYou", "八臂相拥"); put("EightfoldEmbrace", "八重拥抱");
        put("EmptySeat", "空座"); put("EraseThePain", "抹去痛苦");
        put("EveningRain", "暮雨"); put("EveryGoodbye", "每一次告别");
        put("FamiliarAche", "熟悉的痛楚"); put("Fathomless", "无底之深");
        put("FirstBreath", "第一口呼吸"); put("FoamBlanket", "泡沫毯");
        put("FoamCrown", "泡沫王冠"); put("FoamDagger", "泡沫短刃");
        put("FoamGuard", "泡沫护卫"); put("FoamHeart", "泡沫之心");
        put("GentleCurrent", "温柔洋流"); put("HalfRemembered", "半被记起");
        put("Heartstring", "心弦"); put("HeldInFoam", "泡沫中的守护");
        put("HoldOn", "坚持住"); put("HoldTheDawn", "守住黎明");
        put("Homesick", "思乡"); put("InkFleck", "墨点"); put("Keepsake", "纪念物");
        put("LastPhotograph", "最后一张照片"); put("LearnToFloat", "学会漂浮");
        put("LetItDrift", "任其漂流"); put("Lighthouse", "灯塔"); put("Longing", "思念");
        put("LostPostcard", "遗失的明信片"); put("MemoryPalace", "记忆宫殿");
        put("MessageInABottle", "瓶中信"); put("MissingPiece", "缺失的碎片");
        put("NamesInSand", "沙中的名字"); put("NeverForget", "永不遗忘");
        put("NoLongerAlone", "不再孤独"); put("NotYetGoodbye", "还不是告别");
        put("OceanRemembers", "海洋铭记"); put("PearlBreaker", "碎珠者");
        put("PearlThread", "珍珠线"); put("PearlVolley", "珍珠齐射");
        put("PearlescentSoul", "珠光之魂"); put("PocketOfAir", "空气囊");
        put("Pop", "爆裂"); put("PressureWave", "压力波"); put("QuietShore", "寂静海岸");
        put("RelearnLove", "重新学会爱"); put("RememberMe", "记住我");
        put("ReturnToShore", "回到海岸"); put("RippleCut", "涟漪斩"); put("Riptide", "离岸流");
        put("SaltInTheWound", "伤口撒盐"); put("SaltwaterStrike", "盐水打击");
        put("SaltwaterVeil", "盐水帷幕"); put("SeaGlass", "海玻璃");
        put("SeaOfFaces", "面容之海"); put("SeaOfStars", "繁星之海");
        put("SeafoamSteps", "海沫之步"); put("SinkTheName", "沉没之名");
        put("SmallGoodbye", "小小的告别"); put("SoapHalo", "肥皂光环");
        put("SoftLanding", "柔软着陆"); put("SomethingFamiliar", "熟悉之物");
        put("StarlessBite", "无星之噬"); put("StolenMoment", "偷来的片刻");
        put("SupernovaPearl", "超新星珍珠"); put("TheEmptyWorld", "空无一物的世界");
        put("TheOneWhoSavedMe", "拯救我的人"); put("TheSeaBetweenUs", "你我之间的海");
        put("TidalMemory", "潮汐记忆"); put("TidalRecall", "潮汐召回");
        put("TidalSlap", "潮汐拍击"); put("TidePool", "潮池"); put("TideTurns", "潮汐转向");
        put("TomorrowHasRoom", "明日仍有余地"); put("TsunamiOfFoam", "泡沫海啸");
        put("UnderThePier", "码头之下"); put("Undercurrent", "暗流");
        put("UndertowWaltz", "离岸流华尔兹"); put("Unforgotten", "未被遗忘");
        put("Unmake", "解构"); put("UntilWeMeetAgain", "直到再次相见");
        put("UnwrittenLetter", "未写完的信"); put("WarmPebble", "温暖的石子");
        put("WeWereHere", "我们曾在这里"); put("WhiteSquall", "白色暴风");
        put("Whitecap", "白浪");
    }

    private ChineseText() { }
    private static void put(String id, String name) { NAMES.put(id, name); }
    public static Map<String, String> cardNames() { return NAMES; }
    public static String cardName(String id) { return NAMES.get(id); }

    public static boolean enabled() {
        return Settings.language == Settings.GameLanguage.ZHS;
    }

    public static String translate(String input) {
        if (input == null || !enabled()) return input;
        String s = input;
        s = s.replace("Deal {v0} damage per natsu:Bubble to ALL enemies", "每层 natsu:Bubble 对所有敌人造成 {v0} 点伤害");
        s = s.replace("Deal {v1} damage per natsu:Bubble to ALL enemies", "每层 natsu:Bubble 对所有敌人造成 {v1} 点伤害");
        s = s.replace("Deal !D! damage to ALL enemies", "对所有敌人造成 !D! 点伤害");
        s = s.replace("Deal !D! damage", "造成 !D! 点伤害");
        s = s.replace("Deal {v0} damage", "造成 {v0} 点伤害");
        s = s.replace("Deal {v1} damage", "造成 {v1} 点伤害");
        s = s.replace("Deal {v2} damage", "造成 {v2} 点伤害");
        s = s.replace("+1 damage per natsu:Bubble", "每层 natsu:Bubble 额外造成 1 点伤害");
        s = s.replace("+1 damage per natsu:Nostalgia", "每层 natsu:Nostalgia 额外造成 1 点伤害");
        s = s.replace("+2 damage per natsu:Nostalgia", "每层 natsu:Nostalgia 额外造成 2 点伤害");
        s = s.replace("Gain !B! Block", "获得 !B! 点格挡");
        s = s.replace("Gain {v0} Block", "获得 {v0} 点格挡");
        s = s.replace("Gain {v1} Block", "获得 {v1} 点格挡");
        s = s.replace("Gain {v2} Block", "获得 {v2} 点格挡");
        s = s.replace("Gain {v0} Energy", "获得 {v0} 点能量");
        s = s.replace("Gain {v1} Energy", "获得 {v1} 点能量");
        s = s.replace("Gain {v2} Energy", "获得 {v2} 点能量");
        s = s.replace("Gain ", "获得 ");
        s = s.replace("Draw ", "抽 ");
        s = s.replace("Apply ", "施加 ");
        s = s.replace("natsu:Pop: Deal ", "natsu:Pop：造成 ");
        s = s.replace("At the start of combat, ", "战斗开始时，");
        s = s.replace("At the start of your turn, ", "你的回合开始时，");
        s = s.replace("At the end of your turn, ", "你的回合结束时，");
        s = s.replace("The first time you Exhaust a card each turn, ", "你每回合首次消耗牌时，");
        s = s.replace("The first time you Exhaust each turn, ", "你每回合首次消耗牌时，");
        s = s.replace("Whenever you Exhaust a card, ", "每当你消耗一张牌时，");
        s = s.replace("Whenever you play an Attack, ", "每当你打出攻击牌时，");
        s = s.replace("Whenever you play a Skill, ", "每当你打出技能牌时，");
        s = s.replace("Whenever you spend natsu:Nostalgia, ", "每当你消耗 natsu:Nostalgia 时，");
        s = s.replace("Whenever you natsu:Recover a card, ", "每当你 natsu:Recover 一张牌时，");
        s = s.replace("Your first exhausted card each turn grants ", "你每回合首次消耗的牌额外给予");
        s = s.replace("Your natsu:Bubbles grant {v0} additional Block each.", "你的每层 natsu:Bubbles 额外提供 {v0} 点格挡。");
        s = s.replace("Apply ", "施加 ");
        s = s.replace(" to ALL enemies", " 对所有敌人");
        s = s.replace("if you have at least ", "若你至少拥有 ");
        s = s.replace("if you have any ", "若你拥有任何 ");
        s = s.replace("if you can afford it", "若资源足够");
        s = s.replace("natsu:Forget your entire hand", "natsu:Forget 你的整手牌");
        s = s.replace("natsu:Forget ", "natsu:Forget ");
        s = s.replace("natsu:Recover ", "natsu:Recover ");
        s = s.replace(" random exhausted cards", " 张随机已消耗的牌");
        s = s.replace("your entire hand", "你的整手牌");
        s = s.replace("Draw {v0} cards", "抽 {v0} 张牌");
        s = s.replace("Draw {v1} cards", "抽 {v1} 张牌");
        s = s.replace("Draw {v2} cards", "抽 {v2} 张牌");
        s = s.replace(" cards", " 张牌");
        s = s.replace("card(s)", "张牌");
        s = s.replace(" times", " 次");
        s = s.replace("per stack spent", "每消耗一层");
        s = s.replace("per exhausted card", "每张已消耗的牌");
        s = s.replace("per natsu:Nostalgia", "每层 natsu:Nostalgia");
        s = s.replace("per natsu:Bubble", "每层 natsu:Bubble");
        s = s.replace(" natsu:Bubbles", " 层 natsu:Bubbles");
        s = s.replace(" natsu:Bubble", " 层 natsu:Bubble");
        s = s.replace(" natsu:Foam", " 层 natsu:Foam");
        s = s.replace(" natsu:Nostalgia", " 层 natsu:Nostalgia");
        s = s.replace(" natsu:Undertow", " 层 natsu:Undertow");
        s = s.replace(" natsu:Ink", " 层 natsu:Ink");
        s = s.replace("Weak", "虚弱"); s = s.replace("Vulnerable", "易伤");
        s = s.replace("Strength", "力量"); s = s.replace("Dexterity", "敏捷");
        s = s.replace("Buffer", "缓冲"); s = s.replace("Block", "格挡");
        s = s.replace("Energy", "能量");
        s = s.replace("Spend ALL ", "消耗所有 "); s = s.replace("Spend ", "消耗 ");
        s = s.replace("Maximum 20", "最多 20 层");
        s = s.replace("Then lose 1", "然后失去 1 层");
        s = s.replace("Exhaust.", "消耗。"); s = s.replace("Exhaust", "消耗");
        s = s.replace(". NL ", "。 NL "); s = s.replace(". ", "。 ");
        s = s.replace(" and ", " 和 ");
        return s;
    }
}
