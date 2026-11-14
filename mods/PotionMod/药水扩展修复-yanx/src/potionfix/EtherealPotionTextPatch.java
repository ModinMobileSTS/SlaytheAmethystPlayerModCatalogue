package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import potionmod.potions.EtherealPotion;

/**
 * 虚化药水实际给的是永久【腐化】（整场战斗生效），强度保留，
 * 只把“本回合”的描述改成与真实效果一致。
 */
@SpirePatch2(
    cls = "potionmod.potions.EtherealPotion",
    method = "<ctor>",
    paramtypez = {}
)
public final class EtherealPotionTextPatch {
    private EtherealPotionTextPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LocalizedText.potionDescription(EtherealPotion.class, "PotionMod:EtherealPotion", 0,
                "获得 #y腐化 ：技能牌费用变为 #b0 ，使用后 #y消耗 。");
    }
}
