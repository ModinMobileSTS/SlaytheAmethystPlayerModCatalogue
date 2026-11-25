package nobmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.powers.AngerPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import spireTogether.network.P2P.P2PCallbacks;

// Called by the parent mod for teammate plays in the same room and combat action.
@SpirePatch2(clz = P2PCallbacks.class, method = "OnAllyPlayedCard",
        paramtypez = {AbstractCard.class, AbstractCreature.class, Integer.class})
public final class AllySkillPatch {
    @SpirePostfixPatch
    public static void Postfix(Object[] __args) {
        if (!TeamAnger.isSkillPlay((AbstractCard) __args[0])) {
            return;
        }
        AbstractRoom room = TeamAnger.currentBattle();
        if (room == null) {
            return;
        }
        int quota = -1;
        for (AbstractMonster monster : room.monsters.monsters) {
            if (!(monster instanceof GremlinNob) || monster.isDeadOrEscaped()) {
                continue;
            }
            AbstractPower power = monster.getPower(AngerPower.POWER_ID);
            if (!(power instanceof AngerPower)) {
                continue;
            }
            if (quota < 0) {
                quota = TeamAnger.localQuota();
                if (quota < 1) {
                    return;
                }
            }
            TeamAnger.count((AngerPower) power, quota);
        }
    }
}
