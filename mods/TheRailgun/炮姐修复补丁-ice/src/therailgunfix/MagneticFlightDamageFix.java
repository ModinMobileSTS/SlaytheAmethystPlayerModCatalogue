package therailgunfix;

import TheRailgun.powers.MagneticFlightPower;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

@SpirePatch2(clz = MagneticFlightPower.class, method = "onAttacked", paramtypez = {DamageInfo.class, int.class})
public class MagneticFlightDamageFix {
    @SpirePrefixPatch
    public static SpireReturn prefix(Object[] __args) {
        DamageInfo info = (DamageInfo) __args[0];
        int amount = (Integer) __args[1];
        if (info == null || info.owner == null || !(info.owner instanceof AbstractMonster)) {
            return SpireReturn.Return(amount);
        }
        return SpireReturn.Return((int) Math.ceil(amount * 0.5));
    }
}
