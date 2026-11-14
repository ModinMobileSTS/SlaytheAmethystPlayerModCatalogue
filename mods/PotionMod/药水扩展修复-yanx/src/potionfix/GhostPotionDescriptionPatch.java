package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.localization.PotionStrings;
import potionmod.potions.GhostPotion;

/**
 * 补齐文本后，原构造函数仍会把多拼的一个 "2" 写进描述（"…并抽 3 张卡牌。2"）。
 * 这里按本地化文本重新拼装描述，并同步刷新提示框文字，保留其它模组附加的来源前缀。
 */
@SpirePatch2(
    clz = GhostPotion.class,
    method = "<ctor>",
    paramtypez = {}
)
public final class GhostPotionDescriptionPatch {
    private GhostPotionDescriptionPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix(GhostPotion __instance) {
        if (__instance == null) {
            return;
        }
        PotionStrings strings = PotionStringsLookup.ghostStrings();
        if (strings == null || strings.DESCRIPTIONS == null || strings.DESCRIPTIONS.length < 2) {
            return;
        }
        String broken = __instance.description;
        String fixed = strings.DESCRIPTIONS[0] + __instance.getPotency() + strings.DESCRIPTIONS[1];
        __instance.description = fixed;
        if (broken == null || broken.equals(fixed) || __instance.tips == null) {
            return;
        }
        for (PowerTip tip : __instance.tips) {
            if (tip != null && tip.body != null && tip.body.contains(broken)) {
                tip.body = tip.body.replace(broken, fixed);
            }
        }
    }
}
