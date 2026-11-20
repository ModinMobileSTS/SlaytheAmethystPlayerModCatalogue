package patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import java.lang.reflect.Field;
import java.util.Map;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CharacterStrings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.OrbStrings;
import com.megacrit.cardcrawl.localization.StanceStrings;
import com.megacrit.cardcrawl.localization.RelicStrings;
import com.megacrit.cardcrawl.localization.PowerStrings;
import com.megacrit.cardcrawl.localization.TutorialStrings;
import com.megacrit.cardcrawl.localization.EventStrings;
import basemod.BaseMod;

@SpirePatch2(cls = "code.ModFile", method = "receiveEditStrings", paramtypez = {})
public final class ChineseLocalizationPatch {
    @SpirePostfixPatch
    public static void Postfix(code.ModFile __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) return;
        ModMetadataTranslation.apply();
        String vampire = "你走在漆黑无光的街道上，发现几个戴着兜帽的人正在举行黑暗仪式。你走近时，他们齐刷刷地望向你。最高的那人露出尖牙，伸出苍白的手，说： NL #y你 #y听说过 #y道吗？";
        character("TenkoShibuki", "天狐希布基", "终于从神社里爬出来的 #p希布基。", "NL 你释放了作为狐神的神力……", vampire);
        character("HanakoNana", "花子娜娜", "#r娜娜，看前面让我拍张照好吗？ #r前面？", "NL 你装填好爱爱，瞄准心脏……", vampire);
        character("YuzuhaRiko", "柚叶莉可", "击败魔王的秘密武器是 #g美 #g人 #g计？", "NL 你将所有魔力集中于心脏……", vampire);
        character("AokumoRin", "青云凛", "你说我把东西弄乱了？ #b才 #b没 #b有。", "NL 你举起锤子，准备向心脏砸下……", vampire);
        LocalizationOverlay.load(CardStrings.class, "cards", "Cardstrings.json", "cards.json");
        VanillaCardTranslation.apply();
        LocalizationOverlay.load(RelicStrings.class, "relics", "Relicstrings.json", "relics.json");
        LocalizationOverlay.load(PowerStrings.class, "powers", "Powerstrings.json", "powers.json");
        LocalizationOverlay.load(TutorialStrings.class, "tutorials", "Tutorialstrings.json", "tutorials.json");
        LocalizationOverlay.load(EventStrings.class, "events", "Eventstrings.json", "events.json");
        PowerStrings nyoiiig = (PowerStrings) lookup("powers", "Ao_40_Nyoiiig_Power");
        if (nyoiiig != null && nyoiiig.DESCRIPTIONS != null && nyoiiig.DESCRIPTIONS.length == 1) {
            nyoiiig.DESCRIPTIONS = new String[]{nyoiiig.DESCRIPTIONS[0], "回合开始时，获得 #b7 点 #y格挡 ，并将 #b2 张 #y咚恰咚恰 加入手牌。"};
        }
        stance("FoxGodStance", "狐神", "#y神圣 牌变为 #y权能 牌。 NL 每当使用一张 #y权能 牌，失去 1 点 #y神性力。");
        stance("HikikomoriStance", "家里蹲", "#y游戏成瘾、#y魔像 和 #y熊孩子 牌的效果改变。");
        stance("IbericoStance", "伊比利科", "#y吃货 牌的费用变为 0。 NL 使用 #y吃货 牌时将其 #y消化。");
        orb("EmptyStasisSlot", "餐盘", new String[]{"可用于 #y上菜 #y灵魂美食 的位置。"});
        orb("Ao_Chicken", "炸鸡", new String[]{"#y持续：回合结束时，对随机敌人造成 #b", " 点伤害。 NL 每层 #y炸鸡 额外造成 1 点伤害。", " NL #y品尝：对所有敌人施加 #b", " 层 #y炸鸡。", " 对所有敌人造成伤害"});
        orb("Ao_Kiwi", "猕猴桃", new String[]{"#y持续：回合结束时，获得 #b", " 点 #y格挡。", " NL #y品尝：本回合每次失去生命时，少失去 #b", " 点生命。"});
        orb("Ao_ChocolateLatte", "巧克力拿铁", new String[]{"#y持续：回合开始时获得 [E]。 NL 持续效果不受 #y鲜味 影响。", " NL #y品尝：抽 #b", " 张牌。", " 抽牌。"});
    }
    private static Object lookup(String table, String id) {
        try {
            Field f = LocalizedStrings.class.getDeclaredField(table);
            f.setAccessible(true);
            return ((Map<?, ?>) f.get(null)).get("Stellive:" + id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法读取帕斯泰尔语言表: " + table, e);
        }
    }
    private static void character(String id, String name, String intro, String heart, String vampire) {
        CharacterStrings s = (CharacterStrings) lookup("characters", id);
        if (s == null) return;
        if (s.NAMES != null && s.NAMES.length >= 2) { s.NAMES[0] = name; s.NAMES[1] = name; }
        if (s.TEXT != null && s.TEXT.length >= 3) { s.TEXT[0] = intro; s.TEXT[1] = heart; s.TEXT[2] = vampire; }
    }
    private static void stance(String id, String name, String description) {
        StanceStrings s = (StanceStrings) lookup("stance", id);
        if (s != null) { s.NAME = name; if (s.DESCRIPTION != null && s.DESCRIPTION.length > 0) s.DESCRIPTION[0] = description; }
    }
    private static void orb(String id, String name, String[] description) {
        OrbStrings s = (OrbStrings) lookup("orb", id);
        if (s != null) { s.NAME = name; if (s.DESCRIPTION != null && s.DESCRIPTION.length == description.length) System.arraycopy(description, 0, s.DESCRIPTION, 0, description.length); }
    }
}
