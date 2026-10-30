package dnkmodl10n;

import basemod.ModBadge;
import basemod.ModPanel;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import java.lang.reflect.Field;

/**
 * Translates the hover tooltip of the DNKmod badge on the main menu.
 * The tooltip is built as "author NL description" inside the ModBadge constructor,
 * so the stored string is edited in place.
 */
@SpirePatch2(clz = ModBadge.class, method = "<ctor>", paramtypez = {
        Texture.class, float.class, float.class, String.class, String.class, String.class, ModPanel.class})
public class BadgePatch {

    private static final String ENGLISH_DESCRIPTION = "This is just my testing mod.";
    private static final String CHINESE_DESCRIPTION = "这只是我的测试模组。";

    @SpirePostfixPatch
    public static void Postfix(ModBadge __instance) {
        if (__instance == null) {
            return;
        }
        try {
            Field tipField = ModBadge.class.getDeclaredField("tip");
            tipField.setAccessible(true);
            Object value = tipField.get(__instance);
            if (!(value instanceof String)) {
                return;
            }
            String tip = (String) value;
            if (!tip.contains(ENGLISH_DESCRIPTION)) {
                return;
            }
            tipField.set(__instance, tip.replace(ENGLISH_DESCRIPTION, CHINESE_DESCRIPTION));
        } catch (Throwable ignored) {
            // Cosmetic only: never let the translation break badge registration.
        }
    }
}
