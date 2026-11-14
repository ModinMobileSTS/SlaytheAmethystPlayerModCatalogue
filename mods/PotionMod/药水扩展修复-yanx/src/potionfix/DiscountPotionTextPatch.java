package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import potionmod.potions.DiscountPotion;

/**
 * 折扣药水实际是“下回合一次性抽 3 张”（原版 DrawCardNextTurnPower 的行为），
 * 强度按作者原意保留，只把“接下来 3 回合抽 1 张”的错误描述改成实际效果。
 */
@SpirePatch2(
    cls = "potionmod.potions.DiscountPotion",
    method = "<ctor>",
    paramtypez = {}
)
public final class DiscountPotionTextPatch {
    private DiscountPotionTextPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LocalizedText.potionDescription(DiscountPotion.class, "PotionMod:DiscountPotion", 0,
                "获得 #b2 点 #y能量 并在下回合抽 #b");
        LocalizedText.potionDescription(DiscountPotion.class, "PotionMod:DiscountPotion", 1,
                " 张卡牌。");
    }
}
