package sts.ryoikitenkai.compat.red;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;

@SpirePatch2(
    cls = "com.megacrit.cardcrawl.cards.red.Metallicize",
    method = "use",
    paramtypez = { com.megacrit.cardcrawl.characters.AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class }
)
public final class MetallicizeCastPatch {
    private MetallicizeCastPatch() {
    }

    @SpirePrefixPatch
    public static void Prefix(com.megacrit.cardcrawl.cards.red.Metallicize __instance, Object[] __args) {
        RedEffectRegistry.castMetallicize((AbstractPlayer) __args[0]);
    }
}
