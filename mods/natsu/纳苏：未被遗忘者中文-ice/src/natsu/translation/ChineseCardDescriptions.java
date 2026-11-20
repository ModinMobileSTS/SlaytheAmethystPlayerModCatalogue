package natsu.translation;

import com.megacrit.cardcrawl.localization.CardStrings;
import java.util.HashMap;
import java.util.Map;

public final class ChineseCardDescriptions {
    private static final Map<String, String[]> TEXT = new HashMap<String, String[]>();

    static {
        put("SaltwaterStrike", "造成 !D! 点伤害。");
        put("FoamGuard", "获得 !B! 点格挡。");
        put("FirstBreath", "获得 {v0} 层 natsu:Bubbles。 NL natsu:Forget {v1} 张牌。");
        put("Longing", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Nostalgia。");
        put("BubbleNeedle", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("TidalSlap", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("PearlVolley", "造成 !D! 点伤害，共 {v1} 次。");
        put("BrineLash", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Undertow。");
        put("SeaGlass", "造成 !D! 点伤害。");
        put("Whitecap", "对所有敌人造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Foam。");
        put("SmallGoodbye", "造成 !D! 点伤害。 NL natsu:Forget {v1} 张牌。");
        put("NamesInSand", "造成 !D! 点伤害。 NL 施加 {v1} 层虚弱。");
        put("InkFleck", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Ink。");
        put("Undercurrent", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Undertow。");
        put("EmptySeat", "造成 !D! 点伤害。 NL 抽 {v1} 张牌。");
        put("HoldOn", "造成 !D! 点伤害。 NL 获得 !B! 点格挡。");
        put("RippleCut", "造成 !D! 点伤害。 NL 每层 natsu:Bubble 使伤害提高 1 点。");
        put("FamiliarAche", "造成 !D! 点伤害。 NL 每层 natsu:Nostalgia 使伤害提高 1 点。");
        put("DissolvingEdge", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Foam。 NL 施加 {v2} 层 natsu:Ink。");
        put("PocketOfAir", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("FoamBlanket", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Foam。");
        put("SoapHalo", "获得 {v0} 层 natsu:Bubbles。");
        put("LetItDrift", "natsu:Forget {v0} 张牌。 NL 获得 !B! 点格挡。");
        put("QuietShore", "获得 !B! 点格挡。");
        put("BottleASigh", "获得 {v0} 层 natsu:Bubbles。 NL 抽 {v1} 张牌。");
        put("HalfRemembered", "抽 {v0} 张牌。 NL natsu:Forget {v1} 张牌。");
        put("WarmPebble", "获得 {v0} 层 natsu:Nostalgia。 NL 获得 !B! 点格挡。 NL 消耗。");
        put("LostPostcard", "获得 {v0} 层 natsu:Nostalgia。 NL 抽 {v1} 张牌。");
        put("SeafoamSteps", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("TidePool", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Foam。");
        put("Pop", "natsu:Pop：每层 natsu:Bubble 对所有敌人造成 {v0} 点伤害。");
        put("SaltInTheWound", "造成 !D! 点伤害。 NL 施加 {v1} 层易伤。");
        put("DeepExhale", "对所有敌人施加 {v0} 层 natsu:Undertow。 NL 获得 {v1} 层 natsu:Foam。");
        put("ALittleCourage", "获得 !B! 点格挡。 NL natsu:Forget {v1} 张牌。");
        put("MissingPiece", "消耗 {v0} 层 natsu:Nostalgia。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 获得 !B! 点格挡。");
        put("SoftLanding", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Foam。");
        put("Driftwood", "造成 !D! 点伤害。");
        put("PearlThread", "获得 {v0} 层 natsu:Bubbles。 NL 获得 {v1} 层 natsu:Nostalgia。");
        put("BorrowedAir", "获得 {v0} 点能量。 NL natsu:Forget {v1} 张牌。 NL 消耗。");
        put("EveningRain", "对所有敌人造成 !D! 点伤害。");
        put("BubbleGarden", "你的回合开始时，获得 {v0} 层 natsu:Bubbles。");
        put("FoamHeart", "每当你消耗一张牌时，获得 {v0} 层 natsu:Foam。");
        put("Keepsake", "你的回合开始时，获得 {v0} 层 natsu:Nostalgia。");
        put("AnchorOfSelf", "你每回合首次消耗牌时，获得 {v0} 点格挡。");
        put("EchoChamber", "你每回合首次消耗牌时，抽 {v0} 张牌。");
        put("GentleCurrent", "你的回合开始时，对所有敌人施加 {v0} 层 natsu:Undertow。");
        put("ConstellationScars", "每当你消耗一张牌时，对所有敌人造成 {v0} 点伤害。");
        put("Homesick", "你的回合结束时，若至少拥有 3 层 natsu:Nostalgia，获得 {v0} 层 natsu:Bubbles。");
        put("MessageInABottle", "抽 {v0} 张牌。 NL 获得 {v1} 层 natsu:Nostalgia。 NL 消耗。");
        put("RememberMe", "消耗 {v0} 层 natsu:Nostalgia。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 抽 {v2} 张牌。 NL 消耗。");
        put("BorrowedSummer", "消耗 {v0} 层 natsu:Nostalgia。 NL 获得 {v1} 点能量。 NL 抽 {v2} 张牌。 NL 消耗。");
        put("TidalRecall", "消耗 {v0} 层 natsu:Nostalgia。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 获得 {v2} 层 natsu:Bubbles。");
        put("EraseThePain", "natsu:Forget {v0} 张牌。 NL 获得 !B! 点格挡。");
        put("SeaOfFaces", "对所有敌人施加 {v0} 层 natsu:Ink。 NL 获得 {v1} 层 natsu:Nostalgia。");
        put("UnwrittenLetter", "获得 {v0} 层 natsu:Nostalgia。 NL natsu:Forget {v1} 张牌。 NL 消耗。");
        put("DeepShelter", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("HeldInFoam", "获得 {v0} 层 natsu:Foam。");
        put("UnderThePier", "获得 !B! 点格挡。 NL 抽 {v1} 张牌。");
        put("StolenMoment", "消耗 {v0} 层 natsu:Nostalgia。 NL 抽 {v1} 张牌。");
        put("NotYetGoodbye", "获得 !B! 点格挡。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 消耗。");
        put("FoamCrown", "获得 {v0} 层 natsu:Bubbles。 NL 获得 {v1} 层 natsu:Foam。 NL 消耗。", "获得 {v0} 层 natsu:Bubbles。 NL 获得 {v1} 层 natsu:Foam。");
        put("SinkTheName", "造成 !D! 点伤害。 NL natsu:Forget {v1} 张牌。");
        put("EightfoldEmbrace", "造成 !D! 点伤害，共 {v1} 次。 NL 施加 {v2} 层 natsu:Ink。");
        put("PearlBreaker", "造成 !D! 点伤害。 NL natsu:Pop：每层 natsu:Bubble 对所有敌人造成 {v1} 点伤害。 NL 对所有敌人施加 {v2} 层 natsu:Ink。");
        put("UndertowWaltz", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Undertow。");
        put("TidalMemory", "造成 !D! 点伤害。 NL 每层 natsu:Nostalgia 使伤害提高 2 点。 NL 施加 {v1} 层 natsu:Ink。");
        put("StarlessBite", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Ink。");
        put("BrokenPromise", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Nostalgia。 NL 消耗。");
        put("FoamDagger", "造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Foam。 NL 消耗。");
        put("DistantLaughter", "对所有敌人造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Nostalgia。");
        put("WhiteSquall", "对所有敌人造成 !D! 点伤害。 NL 获得 {v1} 层 natsu:Bubbles。 NL 对所有敌人施加 {v2} 层 natsu:Ink。");
        put("SomethingFamiliar", "造成 !D! 点伤害。 NL 每层 natsu:Nostalgia 使伤害提高 1 点。 NL 抽 {v1} 张牌。");
        put("Heartstring", "造成 !D! 点伤害。 NL 施加 {v1} 层虚弱。");
        put("PressureWave", "对所有敌人造成 !D! 点伤害。 NL 每层 natsu:Bubble 使伤害提高 1 点。");
        put("Unmake", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Ink。 NL 消耗。");
        put("Lighthouse", "你的回合结束时，若拥有 natsu:Bubbles，获得 {v0} 点格挡。");
        put("LearnToFloat", "你的每层 natsu:Bubbles 额外提供 {v0} 点格挡。");
        put("SaltwaterVeil", "获得 !B! 点格挡。 NL 对所有敌人施加 {v1} 层虚弱。");
        put("Riptide", "对所有敌人施加 {v0} 层 natsu:Undertow。");
        put("MemoryPalace", "获得 {v0} 层 natsu:Nostalgia。 NL 抽 {v1} 张牌。 NL 消耗。");
        put("TideTurns", "消耗所有 natsu:Nostalgia。每消耗一层，获得 {v0} 点格挡。");
        put("APlaceBesideYou", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Nostalgia。 NL 获得 {v2} 层 natsu:Bubbles。");
        put("BleachedPhotograph", "natsu:Forget {v0} 张牌。 NL 抽 {v1} 张牌。 NL 获得 {v2} 层 natsu:Foam。");
        put("CosmicInk", "对所有敌人施加 {v0} 层 natsu:Undertow。 NL 对所有敌人施加 {v1} 层 natsu:Ink。");
        put("TheOneWhoSavedMe", "你的回合开始时，消耗 2 层 natsu:Nostalgia 以获得 {v0} 层缓冲，若资源足够。");
        put("WeWereHere", "你每回合首次消耗的牌额外给予 {v0} 层 natsu:Nostalgia。");
        put("OceanRemembers", "你的回合结束时，若至少拥有 3 层 natsu:Nostalgia，对所有敌人每层 natsu:Nostalgia 造成 {v0} 点伤害，然后消耗 3 层 natsu:Nostalgia。");
        put("ChosenFamily", "你的回合开始时，获得 {v0} 点格挡和 {v0} 层 natsu:Bubbles。");
        put("NoLongerAlone", "每当你 natsu:Recover 一张牌，获得 {v0x3} 点格挡和 {v0} 层 natsu:Bubble{v0s}。");
        put("PearlescentSoul", "每当你打出一张技能牌，获得 {v0} 层 natsu:Bubble{v0s}。");
        put("ANameOfMyOwn", "消耗 {v0} 层 natsu:Nostalgia。 NL 获得 {v1} 点力量。 NL 获得 {v2} 点敏捷。 NL 消耗。");
        put("ReturnToShore", "消耗 {v0} 层 natsu:Nostalgia。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 获得 {v2} 点能量。 NL 消耗。");
        put("NeverForget", "消耗 {v0} 层 natsu:Nostalgia。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 获得 {v2} 层 natsu:Bubbles。 NL 消耗。");
        put("SeaOfStars", "对所有敌人造成 !D! 点伤害。 NL 对所有敌人施加 {v1} 层 natsu:Undertow。 NL 对所有敌人施加 {v2} 层 natsu:Ink。");
        put("EightArmsToHoldYou", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Foam。 NL 获得 {v2} 层 natsu:Bubbles。 NL 消耗。");
        put("LastPhotograph", "造成 !D! 点伤害。 NL 消耗所有 natsu:Nostalgia。每消耗一层，造成 {v1} 点伤害。 NL 施加 {v2} 层 natsu:Ink。 NL 消耗。");
        put("AbyssalReunion", "造成 !D! 点伤害。 NL 施加 {v1} 层 natsu:Undertow。 NL 施加 {v2} 层 natsu:Ink。");
        put("EveryGoodbye", "造成 !D! 点伤害。 NL 每有一张已消耗的牌，伤害提高 2 点。 NL 施加 {v1} 层 natsu:Ink。");
        put("SupernovaPearl", "natsu:Pop：每层 natsu:Bubble 对所有敌人造成 {v0} 点伤害。 NL 对所有敌人施加 {v1} 层 natsu:Ink。 NL 消耗。");
        put("TheEmptyWorld", "对所有敌人施加 {v0} 层 natsu:Ink。 NL 对所有敌人施加 {v1} 层虚弱。 NL natsu:Forget {v2} 张牌。");
        put("BreatheAgain", "消耗 {v0} 层 natsu:Nostalgia。 NL 获得 {v1} 点能量。 NL 抽 {v2} 张牌。 NL 消耗。");
        put("UntilWeMeetAgain", "获得 !B! 点格挡。 NL natsu:Recover {v1} 张随机已消耗的牌。 NL 消耗。");
        put("TsunamiOfFoam", "获得 !B! 点格挡。 NL 获得 {v1} 层 natsu:Bubbles。");
        put("AllThatRemains", "natsu:Forget 你的整手牌。 NL 抽 {v1} 张牌。 NL 消耗。");
        put("Fathomless", "每当你打出一张攻击牌，对所有敌人施加 {v0} 层 natsu:Undertow。");
        put("HoldTheDawn", "消耗 {v0} 层 natsu:Nostalgia。 NL 获得 {v1} 层缓冲。 NL 获得 {v2} 层 natsu:Bubbles。 NL 消耗。");
        put("RelearnLove", "每当你消耗 natsu:Nostalgia，获得 {v0} 层 natsu:Foam。");
        put("Unforgotten", "造成 !D! 点伤害。 NL 每层 natsu:Nostalgia 使伤害提高 1 点。 NL 获得 {v1} 层 natsu:Nostalgia。 NL 施加 {v2} 层 natsu:Ink。");
        put("TheSeaBetweenUs", "对所有敌人造成 !D! 点伤害。 NL 对所有敌人施加 {v1} 层 natsu:Undertow。 NL 对所有敌人施加 {v2} 层 natsu:Ink。");
        put("TomorrowHasRoom", "获得 {v0} 层 natsu:Nostalgia。 NL 获得 {v1} 层 natsu:Bubbles。 NL 抽 {v2} 张牌。 NL 消耗。");
    }

    private ChineseCardDescriptions() { }

    private static void put(String id, String description) {
        put(id, description, description);
    }

    private static void put(String id, String description, String upgraded) {
        TEXT.put(id, new String[] {description, upgraded});
    }

    public static boolean hasTemplate(String id) {
        return TEXT.containsKey(id);
    }

    public static void apply(String id, CardStrings strings) {
        String[] text = TEXT.get(id);
        if (text != null) {
            strings.DESCRIPTION = format(text[0]);
            strings.UPGRADE_DESCRIPTION = format(text[1]);
        } else {
            strings.DESCRIPTION = format(ChineseText.translate(strings.DESCRIPTION));
            strings.UPGRADE_DESCRIPTION = format(ChineseText.translate(strings.UPGRADE_DESCRIPTION));
        }
    }

    public static String format(String text) {
        if (text == null) return null;
        return text.replace("natsu:Bubble{v0s}", "natsu:Bubbles")
            .replace("natsu:Bubbles", " natsu:气泡 ")
            .replace("natsu:Bubble", " natsu:气泡 ")
            .replace("natsu:Foam", " natsu:泡沫 ")
            .replace("natsu:Nostalgia", " natsu:怀旧 ")
            .replace("natsu:Undertow", " natsu:潮汐暗流 ")
            .replace("natsu:Forget", " natsu:遗忘 ")
            .replace("natsu:Recover", " natsu:召回 ")
            .replace("natsu:Ink", " natsu:墨迹 ")
            .replace("natsu:Pop", " natsu:爆裂 ")
            .replace("点格挡", "点 格挡 ")
            .replace("层虚弱", "层 虚弱 ")
            .replace("层易伤", "层 易伤 ")
            .replace("层缓冲", "层 缓冲 ")
            .replace("点力量", "点 力量 ")
            .replace("点敏捷", "点 敏捷 ")
            .replace("NL 消耗。", "NL 消耗 。");
    }
}
