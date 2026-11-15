package anniv7zhs;

import basemod.ReflectionHacks;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.core.Settings;
import java.util.List;
import java.util.Map;
import spireCafe.interactables.patrons.missingno.MarkovChain;

/**
 * Replaces the English Markov chain word pools used by the Missingno patron with Chinese ones.
 *
 * <p>The relic description, the relic flavor text and the effect line of the Missingno card are
 * not read from a localization file: they are randomly assembled at runtime from the text files
 * {@code anniv7Resources/localization/eng/MissingnoPatron/markov-*.txt}. The chain is built once
 * in the private {@code MarkovChain(MarkovType)} constructor, so this patch rebuilds it from the
 * matching file under {@code localization/zhs/} right after the English one has been loaded.</p>
 *
 * <p>The Chinese pools are pre-segmented with spaces, which is the unit the chain works with and
 * which also keeps the glitched, machine-broken look of the original text. Only the card, relic
 * and flavor pools are replaced: the patron dialogue and the bookshelf page are drawn with the
 * decorative glitch typeface {@code Gridlockd.ttf}, which has no CJK glyphs, so those two stay
 * in their original Latin form instead of rendering as empty boxes.</p>
 */
@SpirePatch2(
    cls = "spireCafe.interactables.patrons.missingno.MarkovChain",
    method = "<ctor>",
    paramtypez = { MarkovChain.MarkovType.class }
)
public final class MissingnoCorpusPatch {

    private static final String ROOT = "anniv7Resources/localization/zhs/MissingnoPatron/";

    private MissingnoCorpusPatch() {
    }

    @SuppressWarnings("unchecked")
    @SpirePostfixPatch
    public static void Postfix(MarkovChain __instance, Object[] __args) {
        if (Settings.language != Settings.GameLanguage.ZHS || __instance == null) {
            return;
        }
        if (__args == null || __args.length < 1 || !(__args[0] instanceof MarkovChain.MarkovType)) {
            return;
        }
        String fileName = corpusFor((MarkovChain.MarkovType) __args[0]);
        if (fileName == null) {
            return;
        }
        try {
            FileHandle handle = Gdx.files.internal(ROOT + fileName);
            if (!handle.exists()) {
                return;
            }
            String text = handle.readString("UTF-8");
            if (text == null || text.trim().isEmpty()) {
                return;
            }
            Map<String, List<String>> chain =
                (Map<String, List<String>>) ReflectionHacks.getPrivate(__instance, MarkovChain.class, "markovChain");
            if (chain == null) {
                return;
            }
            chain.clear();
            __instance.buildChain(text);
        } catch (Throwable ignored) {
            // If the Chinese word pool cannot be read, the English chain loaded by the
            // constructor is left untouched instead of breaking the patron.
        }
    }

    private static String corpusFor(MarkovChain.MarkovType type) {
        switch (type) {
            case CARD:
                return "markov-cards.txt";
            case RELIC:
                return "markov-relics.txt";
            case FLAVOR:
                return "markov-relic-flavors.txt";
            default:
                return null;
        }
    }
}
