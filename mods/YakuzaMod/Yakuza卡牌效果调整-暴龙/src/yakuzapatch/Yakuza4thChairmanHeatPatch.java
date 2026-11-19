package yakuzapatch;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import theYakuza.cards.RarePowers.Yakuza4thChairman;
import theYakuza.powers.ChairmanPower;
import theYakuza.powers.HeatLevelPower;

@SpirePatch2(
    cls = "theYakuza.cards.RarePowers.Yakuza4thChairman",
    method = "use",
    paramtypez = {AbstractPlayer.class, com.megacrit.cardcrawl.monsters.AbstractMonster.class}
)
public final class Yakuza4thChairmanHeatPatch {
    private Yakuza4thChairmanHeatPatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Void> Prefix(Yakuza4thChairman __instance, Object[] __args) {
        // Reimplement the card's original two effects, but grant 2 Heat instead of the hardcoded 3.
        AbstractPlayer p = (AbstractPlayer) __args[0];
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new ChairmanPower(p, p, __instance.magicNumber)));
        AbstractDungeon.actionManager.addToBottom(new ApplyPowerAction(p, p, new HeatLevelPower(p, p, 2)));
        return SpireReturn.Return();
    }
}
