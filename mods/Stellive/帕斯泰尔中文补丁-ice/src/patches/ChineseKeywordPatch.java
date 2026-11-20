package patches;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;

@SpirePatch2(cls = "code.ModFile", method = "receiveEditKeywords", paramtypez = {})
public final class ChineseKeywordPatch {
    @SpirePostfixPatch
    public static void Postfix(code.ModFile __instance) {
        if (Settings.language != Settings.GameLanguage.ZHS) return;
        add("消耗", "如果无法支付所需数值，就不能使用这张牌。");
        add("神性力", "神性力达到或超过 #b10 时进入 #y狐神 状态。 NL 神性力降至 #b-10 或以下时进入 #y家里蹲 状态。");
        add("内力", "提高部分 #y拳法 牌的效果。");
        add("弹匣", "剩余发数会改变 #y爱爱 的伤害。 NL 剩余6发：0伤害。 NL 剩余2至5发：6伤害。 NL 剩余1发：18伤害。");
        add("魔弹", "弹匣中的最后一发 #y爱爱 是魔弹。");
        add("空包弹", "弹匣中的第一发 #y爱爱 是空包弹。");
        add("宅力", "受到攻击时，所受伤害增加等同于宅力的数值。 NL 触发后消失。");
        add("刮伤", "娜娜被刮伤，进入 ㅡㅅㅡ 状态。");
        add("魔力", "#y勇者 牌的伤害和格挡提高，数值等同于魔力。");
        add("青脑草", "战斗开始时，青脑草牌的费用随机变为0至3。");
        add("概念", "费用不会因青脑草而改变。");
        add("饱腹感", "获得 #b10 点饱腹感后进入 #y伊比利科 状态。");
        add("伊比利科", "#y吃货 牌的费用变为0。 NL 吃货牌使用后会被 #y消化 。");
        add("消化", "从本场战斗中移除这张牌。 NL 视作消耗。");
        add("固定", "不受其他效果影响，也不会触发其他效果。");
        add("额外效果", "攻击：对所有敌人造成10点 #y固定 伤害。 NL 技能：抽2张牌。 NL 能力：获得 [E] [E] 。 NL 其他：获得7点格挡。");
        add("复制", "将此牌的1张复制洗入弃牌堆。");
        add("上菜", "将 #y灵魂美食 放入空 #y餐盘 。 NL 如果没有空餐盘，先 #y品尝 最右侧的灵魂美食。");
        add("品尝", "移除 #y灵魂美食 并触发品尝效果。");
        add("鲜味", "提高 #y灵魂美食 的效果。");
        add("炸鸡", "持续：对随机敌人造成伤害，每层 #y炸鸡 增加伤害。 NL 品尝：对所有敌人施加 #y炸鸡 。");
        add("猕猴桃", "持续：获得 #y格挡 。 NL 品尝：每次失去生命时，减少失去的生命。");
        add("巧克力拿铁", "持续：获得 [E] 。 NL 品尝：抽牌。");
        add("餐盘", "可以将 #y灵魂美食 #y上菜 的位置。");
        add("灵魂美食", "可上菜的灵魂美食有 #y炸鸡 、 #y猕猴桃 和 #y巧克力拿铁 。");
    }
    private static void add(String name, String description) {
        BaseMod.addKeyword("stellive", name, new String[]{name}, description);
    }
}
