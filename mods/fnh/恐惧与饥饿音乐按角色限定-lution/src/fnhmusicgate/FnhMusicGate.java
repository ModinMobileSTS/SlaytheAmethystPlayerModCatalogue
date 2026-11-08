package fnhmusicgate;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import fearandhunger.Girl;

/**
 * Decides whether the Fear And Hunger replacement music may be used.
 *
 * <p>Inside a run the live player object is authoritative. Outside a run (main menu, character
 * select, credits, ...) {@link CardCrawlGame#chosenCharacter} is used instead, so the mod's menu
 * theme only plays when the Fear And Hunger character was the last chosen character.</p>
 */
public final class FnhMusicGate {

    private FnhMusicGate() {
    }

    public static boolean useModMusic() {
        AbstractPlayer player = AbstractDungeon.player;
        if (player != null) {
            return player instanceof Girl;
        }
        AbstractPlayer.PlayerClass chosen = CardCrawlGame.chosenCharacter;
        return chosen != null && chosen == Girl.Enums.GIRL;
    }
}
