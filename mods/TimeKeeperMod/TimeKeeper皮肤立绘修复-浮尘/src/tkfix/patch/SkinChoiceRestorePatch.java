package tkfix.patch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.screens.charSelect.CharacterSelectScreen;
import tkfix.SkinFixConfig;

/**
 * 每次进入选人界面时，把上次保存的皮肤序号恢复回来。
 */
@SpirePatch2(clz = CharacterSelectScreen.class, method = "initialize", paramtypez = {})
public class SkinChoiceRestorePatch {

    @SpirePostfixPatch
    public static void Postfix() {
        SkinFixConfig.restoreOnce();
    }
}
