package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import potionmod.powers.StealPower;

/**
 * 偷窃能力原描述“本轮战斗对敌人造成的伤害等同于额外获得的金币”读不通，
 * 也无法说明 7 倍换算，这里改成与实际逻辑一致的说法（强度不变）。
 */
@SpirePatch2(
    cls = "potionmod.powers.StealPower",
    method = "<ctor>",
    paramtypez = { com.megacrit.cardcrawl.core.AbstractCreature.class, int.class }
)
public final class StealPowerTextPatch {
    private StealPowerTextPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        LocalizedText.powerDescription(StealPower.class, "PotionMod:StealPower", 0,
                "你的攻击每造成 #b1 点伤害额外获得 #b7 金币，持续整场战斗。");
    }
}
