package spirepassegg.unlock;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import spirepass.Spirepass;

/**
 * Unlocks the hidden Spirepass level 41 easter egg.
 *
 * Spirepass already draws its level display on the main menu, and the flag is
 * persisted in the Spirepass config, so forcing it on here is enough: getCurrentLevel()
 * returns 41 as soon as easterEggActivated is true, and saveConfig() writes it to disk.
 */
@SpireInitializer
public class EggUnlock {

    public static void initialize() {
        try {
            spirepass.Spirepass.easterEggActivated = true;
            Spirepass.saveConfig();
        } catch (Throwable t) {
            // never break mod loading
        }
    }

    /** Same effect as pressing a button, exposed for later hooks. */
    public static void unlock() {
        try {
            CardCrawlGame.sound.play("UNLOCK_PING");
            spirepass.Spirepass.easterEggActivated = true;
            Spirepass.saveConfig();
        } catch (Throwable t) {
            // ignore
        }
    }
}
