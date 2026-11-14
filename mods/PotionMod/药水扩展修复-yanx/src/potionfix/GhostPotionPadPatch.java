package potionfix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.localization.PotionStrings;
import java.util.Arrays;
import potionmod.potions.GhostPotion;

/**
 * 幽灵药水的本地化文本只有 2 条 DESCRIPTIONS，但构造函数会读取 DESCRIPTIONS[2]，
 * 于是 newInstance() 时抛 ArrayIndexOutOfBoundsException；BaseMod 吞掉该异常后
 * PotionHelper.getPotion 返回 null，图鉴与随机药水列表随后触发空指针。
 * 这里在构造函数体执行前把数组补足，保证对象一定能被创建。
 */
@SpirePatch2(
    clz = GhostPotion.class,
    method = "<ctor>",
    paramtypez = {}
)
public final class GhostPotionPadPatch {
    private static final int REQUIRED_DESCRIPTIONS = 3;

    private GhostPotionPadPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        PotionStrings strings = PotionStringsLookup.ghostStrings();
        if (strings == null || strings.DESCRIPTIONS == null) {
            return;
        }
        if (strings.DESCRIPTIONS.length >= REQUIRED_DESCRIPTIONS) {
            return;
        }
        int oldLength = strings.DESCRIPTIONS.length;
        String[] padded = Arrays.copyOf(strings.DESCRIPTIONS, REQUIRED_DESCRIPTIONS);
        for (int i = oldLength; i < REQUIRED_DESCRIPTIONS; ++i) {
            padded[i] = "";
        }
        strings.DESCRIPTIONS = padded;
    }
}
