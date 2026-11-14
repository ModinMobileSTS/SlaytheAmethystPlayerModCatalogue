package cardrefreshtweak;

import basemod.EasyConfigPanel;
import com.megacrit.cardcrawl.localization.UIStrings;
import java.util.HashMap;

/**
 * Mod settings shown when the mod badge in the main menu is clicked.
 *
 * {@link EasyConfigPanel} reads the stored values into these static fields when the panel is
 * created (during post-initialize) and writes them back whenever a widget is changed, so the
 * values are available to the in-game UI and survive a restart.
 */
public class RefreshConfig extends EasyConfigPanel {

    /** false = reroll the whole card reward (original behaviour), true = reroll single cards. */
    public static boolean singleCardMode = true;
    /** How many times each card of a reward may be rerolled in single-card mode (0 = never). */
    public static int singleCardRefreshes = 1;
    /** true = loading a save gives back the refreshes spent on the reward that is open again. */
    public static boolean refundOnReload = true;

    public RefreshConfig() {
        super("cardrefresh", buildStrings(), "config");
        setNumberRange("singleCardRefreshes", 0.0f, 3.0f);
    }

    /** 0..3, anything else (a hand edited file) is clamped. */
    public static int refreshesPerCard() {
        return Math.max(0, Math.min(3, singleCardRefreshes));
    }

    public static boolean singleModeActive() {
        return singleCardMode && refreshesPerCard() > 0;
    }

    private static UIStrings buildStrings() {
        UIStrings strings = new UIStrings();
        strings.TEXT = new String[0];
        strings.EXTRA_TEXT = new String[0];
        HashMap<String, String> dict = new HashMap<String, String>();
        dict.put("singleCardMode", "单卡刷新模式（关闭 = 整组刷新）");
        dict.put("singleCardRefreshes", "每张卡可刷新次数（0~3，0 = 不刷新）");
        dict.put("refundOnReload", "读档后返还已用刷新次数（SL 复位）");
        strings.TEXT_DICT = dict;
        return strings;
    }
}
