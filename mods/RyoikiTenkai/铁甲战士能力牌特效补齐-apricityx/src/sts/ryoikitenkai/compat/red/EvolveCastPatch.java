package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Evolve",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class EvolveCastPatch {
    private EvolveCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(com.megacrit.cardcrawl.cards.red.Evolve __instance, Object[] __args) {
        RedEffectRegistry.castEvolve((AbstractPlayer) __args[0]);
    }
}
