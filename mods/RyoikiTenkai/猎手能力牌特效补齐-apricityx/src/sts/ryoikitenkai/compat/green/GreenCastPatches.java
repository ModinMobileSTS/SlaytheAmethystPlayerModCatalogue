package sts.ryoikitenkai.compat.green;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

/**
 * 四张能力牌的施放视觉。
 * <p>
 * 一律用 Prefix（不替换原方法）：卡牌结算照常，这里只插一段视觉；
 * 卡牌实例传下去，是为了让 {@code CardFlashVfx} 能在卡牌自己的位置画那张背景剪影。
 */
public final class GreenCastPatches {
    private GreenCastPatches() {
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.cards.green.Accuracy",
        method = "use",
        paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
    )
    public static class AccuracyCast {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.cards.green.Accuracy __instance, Object[] __args) {
            GreenEffectRegistry.castAccuracy((AbstractPlayer) __args[0], __instance);
        }
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.cards.green.InfiniteBlades",
        method = "use",
        paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
    )
    public static class InfiniteBladesCast {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.cards.green.InfiniteBlades __instance, Object[] __args) {
            GreenEffectRegistry.castInfiniteBlades((AbstractPlayer) __args[0], __instance);
        }
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.cards.green.ToolsOfTheTrade",
        method = "use",
        paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
    )
    public static class ToolsTradeCast {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.cards.green.ToolsOfTheTrade __instance, Object[] __args) {
            GreenEffectRegistry.castToolsOfTheTrade((AbstractPlayer) __args[0], __instance);
        }
    }

    @SpirePatch2(
        cls = "com.megacrit.cardcrawl.cards.green.WellLaidPlans",
        method = "use",
        paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
    )
    public static class RetainCast {
        @SpirePrefixPatch
        public static void Prefix(com.megacrit.cardcrawl.cards.green.WellLaidPlans __instance, Object[] __args) {
            GreenEffectRegistry.castRetainCards((AbstractPlayer) __args[0], __instance);
        }
    }
}
