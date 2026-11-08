package fnhmusicgate;

import com.badlogic.gdx.audio.Music;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;

/**
 * Fear And Hunger replaces the shop, shrine, mind bloom, elite, boss and credits music for every
 * character through a postfix on {@code TempMusic.getSong(String)}.
 *
 * <p>This prefix runs first and, whenever the Fear And Hunger character is not the active
 * character, returns the untouched result of the vanilla method ({@code __args[0]}), which skips
 * the replacement logic of the original mod entirely.</p>
 */
@SpirePatch2(
    cls = "fearandhunger.patches.FnhTempMusic",
    method = "Postfix",
    paramtypez = { com.badlogic.gdx.audio.Music.class, com.megacrit.cardcrawl.audio.TempMusic.class, java.lang.String.class }
)
public final class FnhTempMusicGate {
    private FnhTempMusicGate() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Music> Prefix(Object[] __args) {
        Music vanilla = __args.length > 0 ? (Music) __args[0] : null;
        if (vanilla == null || FnhMusicGate.useModMusic()) {
            return SpireReturn.Continue();
        }
        return SpireReturn.Return(vanilla);
    }
}
