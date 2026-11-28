package yuaifix;

import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

/** The only state machine: decide when the track plays, pauses or stops. */
public final class Flow {
    private static final long RETRY_BACKOFF = 2000L;

    private static volatile boolean active;
    private static volatile boolean pausedOffGame;
    private static volatile boolean wasInRun;
    private static long lastFailure;

    private Flow() {
    }

    public static synchronized void evaluate() {
        evaluate(null);
    }

    public static synchronized void evaluate(AbstractPlayer player) {
        boolean inRun = isInRun();
        if (inRun != wasInRun) {
            wasInRun = inRun;
            if (inRun) {
                Music.discard();
                active = false;
                pausedOffGame = false;
                Ducker.set(false);
            }
        }

        AbstractPlayer target = player == null ? AbstractDungeon.player : player;
        boolean low = inRun && belowHalf(target);

        if (!low) {
            if (active) {
                if (inRun) {
                    Music.stop();
                    pausedOffGame = false;
                } else {
                    Music.pause();
                    pausedOffGame = true;
                }
                active = false;
            }
            Ducker.set(false);
            return;
        }

        if (active) {
            if (Music.playing()) {
                Ducker.set(true);
                return;
            }
            active = false;
        }

        if (System.currentTimeMillis() - lastFailure < RETRY_BACKOFF) {
            return;
        }
        if (Music.play(pausedOffGame)) {
            active = true;
            pausedOffGame = false;
            Ducker.set(true);
        } else {
            lastFailure = System.currentTimeMillis();
            Ducker.set(false);
        }
    }

    public static boolean active() {
        return active;
    }

    public static void duck(boolean value) {
        Ducker.set(value);
    }

    public static void tick() {
        Ducker.tick();
    }

    public static void sync() {
        Music.syncVolume();
    }

    private static boolean isInRun() {
        CardCrawlGame.GameMode mode = CardCrawlGame.mode;
        return mode == CardCrawlGame.GameMode.GAMEPLAY
                || mode == CardCrawlGame.GameMode.DUNGEON_TRANSITION;
    }

    private static boolean belowHalf(AbstractPlayer player) {
        if (player == null || player.maxHealth <= 0 || player.currentHealth <= 0) {
            return false;
        }
        return player.currentHealth * 2 < player.maxHealth;
    }
}
