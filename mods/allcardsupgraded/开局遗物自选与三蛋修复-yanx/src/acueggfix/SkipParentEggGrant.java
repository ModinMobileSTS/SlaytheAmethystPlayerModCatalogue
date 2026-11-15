package acueggfix;

import allcardsupgraded.AllCardsUpgradedMod;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * 让父 mod 那套有 bug 的 grantEggsAndUpgradeStartingDeck() 完全跳过，
 * 由 EggGrantFix 用修正后的逻辑接管（去重、只开局发放、清理所有遗物池）。
 *
 * 注意：ModTheSpire 的 Prefix 用 boolean 返回值并不能跳过原方法，
 * 必须返回 SpireReturn 且 isPresent() 为 true 才会提前返回。
 */
@SpirePatch2(clz = AllCardsUpgradedMod.class, method = "grantEggsAndUpgradeStartingDeck", paramtypez = {})
public class SkipParentEggGrant {

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix() {
        return SpireReturn.Return();
    }
}
