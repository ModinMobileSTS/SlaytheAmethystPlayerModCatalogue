package thehighwaymanzhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import theHighwayman.DefaultMod;

/**
 * 语言为简体中文时，改为载入中文关键字，跳过本 Mod 的英文关键字载入。
 */
@SpirePatch2(clz = DefaultMod.class, method = "receiveEditKeywords", paramtypez = {})
public final class KeywordsPatch {
    private KeywordsPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn Prefix() {
        if (!ZhsLocalization.isChinese()) {
            return SpireReturn.Continue();
        }
        if (ZhsLocalization.loadKeywords()) {
            // 载入成功：跳过原方法，避免英文关键字覆盖中文关键字。
            return SpireReturn.Return();
        }
        // 载入失败时让原版英文关键字照常载入。
        return SpireReturn.Continue();
    }
}
