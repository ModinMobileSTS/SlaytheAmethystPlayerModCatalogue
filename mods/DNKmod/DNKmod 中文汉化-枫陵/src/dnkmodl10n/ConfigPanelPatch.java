package dnkmodl10n;

import basemod.BaseMod;
import basemod.IUIElement;
import basemod.ModLabel;
import basemod.ModLabeledToggleButton;
import basemod.ModPanel;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

/**
 * Translates the labels of the DNKmod config panel when the mod badge (and therefore
 * the finished panel) is registered with BaseMod.
 */
@SpirePatch2(
        clz = BaseMod.class,
        method = "registerModBadge",
        paramtypez = {Texture.class, String.class, String.class, String.class, ModPanel.class}
)
public class ConfigPanelPatch {

    private static final String PARENT_MOD_NAME = "DNKmod";

    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        if (__args == null || __args.length < 5) {
            return;
        }
        if (!PARENT_MOD_NAME.equals(__args[1])) {
            return;
        }
        if (!(__args[4] instanceof ModPanel)) {
            return;
        }
        ModPanel panel = (ModPanel) __args[4];
        for (IUIElement element : panel.getUIElements()) {
            if (!(element instanceof ModLabeledToggleButton)) {
                continue;
            }
            ModLabeledToggleButton button = (ModLabeledToggleButton) element;
            ModLabel label = button.text;
            if (label == null || label.text == null) {
                continue;
            }
            String translated = Translations.panelLabel(label.text);
            if (translated == null || translated.equals(label.text)) {
                continue;
            }
            label.text = translated;
            if (button.toggle != null) {
                button.toggle.wrapHitboxToText(translated, 1000.0f, 0.0f, label.font);
            }
        }
    }
}
