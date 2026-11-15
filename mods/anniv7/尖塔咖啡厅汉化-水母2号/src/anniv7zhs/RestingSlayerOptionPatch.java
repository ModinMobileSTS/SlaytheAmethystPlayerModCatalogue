package anniv7zhs;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import java.util.ArrayList;
import spireCafe.ui.Dialog;
import spireCafe.ui.OptionButton;

/**
 * Cleans up the spacing of the Resting Slayer trade options.
 *
 * <p>The option text is a localization template with the relic / card names inserted through
 * {@code FontHelper.colorString(..., "r"/"g")}, which produces words like {@code #rCommon}. The
 * cafe dialog only treats a space separated word as a colour code when it starts with '#', so
 * every inserted name has to be preceded by a space to be coloured instead of printed as plain
 * text. That leaves the Chinese line looking like "remove a common card" with a space between
 * every word.</p>
 *
 * <p>Since consecutive words that share the same colour code are rendered in exactly that same
 * colour anyway, they can be merged back into a single word: the leading colour code is kept and
 * the redundant ones are dropped, which yields natural Chinese without changing any colour.</p>
 */
@SpirePatch2(
    cls = "spireCafe.interactables.patrons.restingslayer.RestingSlayerCutscene",
    method = "onClick",
    paramtypez = {}
)
public final class RestingSlayerOptionPatch {

    private RestingSlayerOptionPatch() {
    }

    @SpirePostfixPatch
    public static void Postfix() {
        if (Settings.language != Settings.GameLanguage.ZHS) {
            return;
        }
        ArrayList<OptionButton> options = Dialog.optionList;
        for (int i = 0; i < options.size(); ++i) {
            OptionButton button = options.get(i);
            if (button != null) {
                button.msg = joinSameColorWords(button.msg);
            }
        }
    }

    private static String joinSameColorWords(String msg) {
        if (msg == null || msg.indexOf('#') < 0) {
            return msg;
        }
        StringBuilder sb = new StringBuilder(msg.length());
        char activeColor = 0;
        boolean first = true;
        for (String word : msg.split(" ")) {
            if (word.isEmpty()) {
                continue;
            }
            char color = 0;
            String body = word;
            if (word.length() >= 2 && word.charAt(0) == '#') {
                char code = word.charAt(1);
                if (code == 'r' || code == 'g' || code == 'b' || code == 'y' || code == 'p') {
                    color = code;
                    body = word.substring(2);
                }
            }
            if (!first && color != 0 && color == activeColor) {
                sb.append(body);
            } else {
                if (!first) {
                    sb.append(' ');
                }
                sb.append(word);
            }
            activeColor = color;
            first = false;
        }
        return sb.toString();
    }
}
