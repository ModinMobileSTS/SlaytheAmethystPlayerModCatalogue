package yuaifix;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/** Thin facades over the parent mod's entry points; no other game changes. */
public final class Patches {
    private Patches() {
    }

    @SpirePatch2(cls = "yuaimod.YuAiController", method = "evaluate", paramtypez = {})
    public static class Evaluate {
        @SpirePrefixPatch
        public static SpireReturn prefix() {
            Flow.evaluate(null);
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(cls = "yuaimod.YuAiController", method = "evaluate",
            paramtypez = {AbstractPlayer.class})
    public static class EvaluatePlayer {
        @SpirePrefixPatch
        public static SpireReturn prefix(Object[] __args) {
            Flow.evaluate((AbstractPlayer) __args[0]);
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(cls = "yuaimod.YuAiController", method = "isActive", paramtypez = {})
    public static class IsActive {
        @SpirePrefixPatch
        public static SpireReturn prefix() {
            return SpireReturn.Return(Flow.active());
        }
    }

    @SpirePatch2(cls = "yuaimod.YuAiBgm", method = "setDucked", paramtypez = {boolean.class})
    public static class SetDucked {
        @SpirePrefixPatch
        public static SpireReturn prefix(Object[] __args) {
            Flow.duck(((Boolean) __args[0]).booleanValue());
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(cls = "yuaimod.YuAiBgm", method = "tick", paramtypez = {})
    public static class Tick {
        @SpirePrefixPatch
        public static SpireReturn prefix() {
            Flow.tick();
            return SpireReturn.Return(null);
        }
    }

    @SpirePatch2(cls = "yuaimod.YuAiMusic", method = "syncVolume", paramtypez = {})
    public static class SyncVolume {
        @SpirePrefixPatch
        public static SpireReturn prefix() {
            Flow.sync();
            return SpireReturn.Return(null);
        }
    }
}
