package thehighwaymanzhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import theHighwayman.DefaultMod;

/**
 * 语言为简体中文时，改为载入中文文本，跳过本 Mod 的英文文本载入。
 */
@SpirePatch2(clz = DefaultMod.class, method = "receiveEditStrings", paramtypez = {})
public final class StringsPatch {
    private StringsPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhsLocalization.isChinese()) {
            return SpireReturn.Continue();
        }
        if (ZhsLocalization.loadAllStrings()) {
            // 载入成功：跳过原方法，避免英文文本覆盖中文文本。
            return SpireReturn.Return();
        }
        // 载入失败时让原版英文文本照常载入，避免出现空白文本。
        return SpireReturn.Continue();
    }
}
