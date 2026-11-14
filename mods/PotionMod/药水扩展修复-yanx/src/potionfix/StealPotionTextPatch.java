package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import potionmod.potions.StealPotion;

/**
 * 偷窃药水实际是整个战斗持续生效（StealPower 不会到期移除），强度保留，
 * 只把“本回合内”改成与能力文本一致的“本场战斗”。
 */
@SpirePatch2(
    cls = "potionmod.potions.StealPotion",
    method = "<ctor>",
    paramtypez = {}
)
public final class StealPotionTextPatch {
    private StealPotionTextPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LocalizedText.potionDescription(StealPotion.class, "PotionMod:StealPotion", 0,
                "本场战斗中，你的攻击每造成 #b1 点伤害额外获得 #b7 金币。");
    }
}
