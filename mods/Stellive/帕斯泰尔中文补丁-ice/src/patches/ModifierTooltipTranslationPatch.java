package patches;

import basemod.helpers.TooltipInfo;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatches2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import java.util.List;

@SpirePatches2({
    @SpirePatch2(cls = "code.CardModifier.AliasModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.CheongchoModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.DivinityModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.IbelikoModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.LoveModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.MagicModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.MaidModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.OtakuModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.SoulfoodModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.SoundeffectModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.WarrantModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.YongsaModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class}),
    @SpirePatch2(cls = "code.CardModifier.geulgModifier", method = "additionalTooltips", paramtypez = {AbstractCard.class})
})
public final class ModifierTooltipTranslationPatch {
    @SpirePostfixPatch
    public static List<TooltipInfo> Postfix(List<TooltipInfo> __result) {
        if (Settings.language != Settings.GameLanguage.ZHS || __result == null) return __result;
        List<TooltipInfo> translated = null;
        for (int i = 0; i < __result.size(); i++) {
            TooltipInfo tip = __result.get(i);
            if (tip == null || tip.title == null || tip.description == null) continue;
            TooltipInfo replacement = translate(tip);
            if (replacement != null) {
                if (translated == null) translated = new ArrayList<>(__result);
                translated.set(i, replacement);
            }
        }
        return translated == null ? __result : translated;
    }

    private static TooltipInfo translate(TooltipInfo tip) {
        String title;
        String description;
        switch (tip.title) {
            case "별명":
                title = "昵称"; description = "昵称牌大多为0费。"; break;
            case "뇌청초":
                title = "青脑草"; description = "[Stellive:CheongchoIcon] 战斗开始时，牌的费用随机改变；部分效果与此特性有关。"; break;
            case "신성":
                title = "神圣"; description = "进入狐神状态后，这张牌会得到强化。"; break;
            case "이베리코":
                title = "伊比利科"; description = "[Stellive:IbelikoIcon] #y消化 这张牌以触发特殊效果。"; break;
            case "사랑이":
                title = "爱爱"; description = "爱爱的伤害随弹匣剩余发数改变。"; break;
            case "메이드":
                title = "女仆"; description = "女仆牌有助于触发灵魂美食的品尝等效果。"; break;
            case "오타쿠":
                title = "宅力"; description = "向敌人施加宅力，造成额外伤害。"; break;
            case "소울푸드":
                title = "灵魂美食"; description = "有助于上菜炸鸡、猕猴桃和巧克力拿铁。"; break;
            case "효과음":
                title = "音效"; description = "什么？这个音效不是循环视频吗？"; break;
            case "권능":
                title = "权能"; description = "进入狐神状态后，牌得到了强化。"; break;
            case "용사":
                title = "勇者";
                if (tip.description.contains("방어도")) {
                    description = "[Stellive:YongsaIcon] #y勇者 牌的伤害和 #y格挡 提高，数值等同于魔力。";
                } else if (tip.description.contains("공격 피해량")) {
                    description = "[Stellive:YongsaIcon] 增加勇者牌造成的攻击伤害。";
                } else {
                    return null;
                }
                break;
            case "긁":
                title = "刮伤"; description = "娜娜被刮伤时会受到伤害并变强。"; break;
            default: return null;
        }
        return new TooltipInfo(title, description);
    }
}
