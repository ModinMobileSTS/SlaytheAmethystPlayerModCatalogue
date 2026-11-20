package natsu.translation;

import basemod.BaseMod;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import java.util.Map;
import natsu.Catalog;
import natsu.SeaPower;

public final class ChineseLocalization {
    private static final String[][] POWER_TEXT = {
        {"bond", "借来的生命", "每当你消耗一张牌时，获得 1 层怀旧。主动遗忘也会改变一段记忆并引爆潮汐暗流。"},
        {"bubble", "气泡", "回合结束时，每层气泡使你获得 2 点格挡，然后失去 1 层。最多 20 层。"},
        {"nost", "怀旧", "消耗这些记忆以重新建立联系。最多 20 层。战斗结束后消失。"},
        {"foam", "泡沫", "反弹受到攻击伤害的 #b{pct}%（包括被格挡的伤害）。每轮失去 1 层。最多 20 层。"},
        {"undertow", "潮汐暗流", "Natsu 主动遗忘一张牌时，受到 #b{n} 点无视格挡的伤害，然后保留一半层数（向下取整）。"},
        {"ink", "墨迹", "每张攻击牌有 #b{pct}% 几率未命中。每轮失去 1 层。达到 20 层时，消耗墨迹并跳过敌人的下一回合。"},
        {"dazed", "迷失于墨色", "跳过你的下一个回合。墨迹已被消耗以触发此效果。"},
        {"garden", "气泡花园", "你的回合开始时，获得 #b{n} 层气泡。"},
        {"foamheart", "泡沫之心", "每当你消耗一张牌时，获得 #b{n} 层泡沫。"},
        {"keepsake", "纪念物", "你的回合开始时，获得 #b{n} 层怀旧。"},
        {"anchor", "自我之锚", "你每回合首次消耗牌时，获得 #b{n} 点格挡。"},
        {"echo", "回声室", "你每回合首次消耗牌时，抽 #b{n} 张牌。"},
        {"current", "温柔洋流", "你的回合开始时，对所有敌人施加 #b{n} 层潮汐暗流。"},
        {"starlight", "星座伤痕", "每当你消耗一张牌时，对所有敌人造成 #b{n} 点伤害。"},
        {"homesick", "思乡", "你的回合结束时，若至少拥有 3 层怀旧，获得 #b{n} 层气泡。"},
        {"lighthouse", "灯塔", "你的回合结束时，若拥有气泡，获得 #b{n} 点格挡。"},
        {"buoyancy", "学会漂浮", "回合结束时，每层气泡额外提供 #b{n} 点格挡。"},
        {"savior", "拯救我的人", "你的回合开始时，消耗 2 层怀旧以获得 #b{n} 层缓冲，若资源足够。"},
        {"wewere", "我们曾在这里", "你每回合首次消耗的牌额外给予 #b{n} 层怀旧。"},
        {"ocean", "海洋铭记", "你的回合结束时，若至少拥有 3 层怀旧，对所有敌人每层怀旧造成 #b{n} 点伤害，然后消耗 3 层怀旧。"},
        {"family", "选择的家人", "你的回合开始时，获得 #b{n} 点格挡和 #b{n} 层气泡。"},
        {"together", "不再孤独", "每当你召回一张牌，获得 #b{triple} 点格挡和 #b{n} 层气泡。"},
        {"soul", "珠光之魂", "每当你打出一张技能牌，获得 #b{n} 层气泡。"},
        {"fathomless", "无底之深", "每当你打出一张攻击牌，对所有敌人施加 #b{n} 层潮汐暗流。"},
        {"relearn", "重新学会爱", "每当你消耗怀旧时，获得 #b{n} 层泡沫。"}
    };

    private static boolean reported;

    private ChineseLocalization() { }

    public static void applyCatalogNames() {
        if (!ChineseText.enabled()) return;
        Catalog.load();
        for (Map.Entry<String, String> entry : ChineseText.cardNames().entrySet()) {
            Catalog.CardDef def = Catalog.cards.get(entry.getKey());
            if (def != null) def.name = entry.getValue();
        }
    }

    public static void applyStrings() {
        if (!ChineseText.enabled() || CardCrawlGame.languagePack == null) return;
        applyCatalogNames();
        for (Map.Entry<String, String> entry : ChineseText.cardNames().entrySet()) {
            CardStrings strings = CardCrawlGame.languagePack.getCardStrings("natsu:" + entry.getKey());
            if (strings == null) continue;
            strings.NAME = entry.getValue();
            ChineseCardDescriptions.apply(entry.getKey(), strings);
        }

        CharacterStrings character = CardCrawlGame.languagePack.getCharacterString("natsu:Natsu");
        if (character != null) {
            character.NAMES = new String[] {"Natsu", "未被遗忘者"};
            character.TEXT = new String[] {"被宇宙乌贼所救，却被世界遗忘。", "Natsu", "你的内心仍记得那片海岸。"};
        }

        relic("NamelessShell", "无名贝壳", "战斗开始时，获得 2 层 natsu:Bubbles。", "曾有人把它放在耳边。她希望那是个善良的人。");
        relic("TidewornLocket", "潮痕吊坠", "战斗开始时，获得 3 层 natsu:Nostalgia。", "面容已逝，温暖仍在。");
        relic("FoamRibbon", "泡沫缎带", "你的回合开始时，获得 1 层 natsu:Foam。", "为无法停留之物系上一只蝴蝶结。");
        relic("MessageBottle", "漂流瓶", "你每回合首次召回一张已消耗的牌时，抽 1 张牌。", "如果你找到了这个，请回信。");
        relic("TwoTeacups", "两只茶杯", "你每消耗第三张牌时，获得 1 点能量。", "她仍然会准备两人份。");
        relic("UnsentPostcard", "未寄出的明信片", "你每回合首次消耗 natsu:Nostalgia 时，获得 2 层 natsu:Bubbles。", "真希望你也在这里。无论你是谁。");
        relic("CosmicConch", "宇宙海螺", "替代无名贝壳。战斗开始时获得 5 层 natsu:Bubbles。你每回合首次消耗牌时，获得 5 点格挡和 1 层 natsu:Nostalgia。", "它救下她之前，从未询问她的名字。");
        relic("AnchorOfTomorrow", "明日之锚", "每回合获得 1 点能量。你的回合开始时，若拥有 natsu:Nostalgia，则失去 1 层。", "未来还没有忘记她。");

        if (!reported) {
            reported = true;
            int missingNames = 0;
            int missingDescriptions = 0;
            for (String id : Catalog.cards.keySet()) {
                if (ChineseText.cardName(id) == null) {
                    ++missingNames;
                    System.out.println("[Natsu ZHS] Missing card name: " + id);
                }
                if (!ChineseCardDescriptions.hasTemplate(id)) {
                    ++missingDescriptions;
                    System.out.println("[Natsu ZHS] Missing card description: " + id);
                }
            }
            CardStrings sample = CardCrawlGame.languagePack.getCardStrings("natsu:SaltwaterStrike");
            System.out.println("[Natsu ZHS] strings applied; cards=" + Catalog.cards.size()
                + ", missing names=" + missingNames + ", missing descriptions=" + missingDescriptions
                + ", sample=" + sample.NAME + " / " + sample.DESCRIPTION);
        }
    }

    private static void relic(String id, String name, String description, String flavor) {
        RelicStrings strings = CardCrawlGame.languagePack.getRelicStrings("natsu:" + id);
        if (strings != null) {
            strings.NAME = name;
            strings.DESCRIPTIONS = new String[] {ChineseCardDescriptions.format(description).replace(" natsu:", " #ynatsu:")};
            strings.FLAVOR = flavor;
        }
    }

    public static void applyKeywords() {
        if (!ChineseText.enabled()) return;
        BaseMod.addKeyword("natsu", "气泡", new String[] {"气泡", "bubbles", "bubble"}, "回合结束时，每层气泡使你获得 2 点格挡，然后失去 1 层气泡。最多 20 层。爆裂会消耗所有气泡，对所有敌人造成伤害。");
        BaseMod.addKeyword("natsu", "泡沫", new String[] {"泡沫", "foam"}, "每层使受到的攻击伤害（包括被格挡的伤害）反弹 5%。每轮失去 1 层。最多 20 层。");
        BaseMod.addKeyword("natsu", "遗忘", new String[] {"遗忘", "forget", "forgetting"}, "主动消耗一张牌，并引爆所有潮汐暗流。一项效果的数值会变为 ?。召回时，该数值会锁定为原数值的 50% 或 150%，并向上取整为正整数。能量费用不会改变。");
        BaseMod.addKeyword("natsu", "怀旧", new String[] {"怀旧", "nostalgia"}, "每当你消耗一张牌时获得的战斗资源。最多 20 层。消耗它来召回牌并加深羁绊。");
        BaseMod.addKeyword("natsu", "潮汐暗流", new String[] {"潮汐暗流", "undertow"}, "只有主动遗忘才会引爆它：造成等同于层数的伤害，无视格挡，然后保留一半层数（向下取整）。");
        BaseMod.addKeyword("natsu", "墨迹", new String[] {"墨迹", "ink"}, "每层使每张攻击牌有 5% 几率未命中。每轮失去 1 层。达到 20 层时，消耗墨迹并跳过敌人的下一回合。");
        BaseMod.addKeyword("natsu", "召回", new String[] {"召回", "recover", "recovered"}, "将一张随机已消耗的牌放回手牌，并保留其升级。超出手牌上限的牌会进入弃牌堆。");
        BaseMod.addKeyword("natsu", "爆裂", new String[] {"爆裂", "pop"}, "消耗所有气泡，对所有敌人造成每层气泡对应的伤害。该伤害无视力量和虚弱。");
        for (String[] row : POWER_TEXT) {
            SeaPower.TEXT.put(row[0], new String[] {row[1], row[2]});
        }
    }
}
