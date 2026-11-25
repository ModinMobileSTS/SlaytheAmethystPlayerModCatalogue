package nobmp;

import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.exordium.GremlinNob;
import com.megacrit.cardcrawl.powers.AngerPower;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import java.util.Iterator;
import java.util.Objects;
import java.util.WeakHashMap;
import spireTogether.SpireTogetherMod;
import spireTogether.network.P2P.P2PCallbacks;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.P2P.P2PMessageSender;
import spireTogether.network.P2P.P2PPlayer;
import spireTogether.network.objects.entities.NetworkPower;
import spireTogether.network.objects.rooms.NetworkLocation;
import spireTogether.other.RoomDataManager;
import spireTogether.util.SpireHelp;

final class TeamAnger {
    // Each new Anger power starts at zero, even in a rematch in the same map room.
    private static final WeakHashMap<AngerPower, Integer> PENDING = new WeakHashMap<>();

    private TeamAnger() {}

    static boolean isSkillPlay(AbstractCard card) {
        return card != null && card.type == AbstractCard.CardType.SKILL && !card.dontTriggerOnUseCard;
    }

    static AbstractRoom currentBattle() {
        if (!SpireTogetherMod.isConnected || !SpireHelp.Gameplay.IsInRun()
                || AbstractDungeon.actionManager == null) {
            return null;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room != null && room.phase == AbstractRoom.RoomPhase.COMBAT && !room.isBattleOver
                && room.monsters != null ? room : null;
    }

    static boolean isActive(AngerPower power) {
        AbstractRoom room = currentBattle();
        return room != null && power.owner instanceof GremlinNob && !power.owner.isDeadOrEscaped()
                && room.monsters.monsters.contains(power.owner)
                && power.owner.getPower(AngerPower.POWER_ID) == power;
    }

    // Only the elected participant consumes the tally. In a split party, count
    // the participants in this encounter, not players in another room or action.
    static int localQuota() {
        NetworkLocation location = SpireHelp.Gameplay.GetMapLocation(false);
        if (location == null || P2PManager.selfID == null) {
            return 0;
        }
        Integer generator = RoomDataManager.GetRoomGenerator(location);
        Integer lowest = null;
        boolean generatorInBattle = false;
        int players = 0;
        Iterator<P2PPlayer> it = P2PManager.GetAllPlayers(true);
        while (it.hasNext()) {
            P2PPlayer player = it.next();
            if (player == null || player.id == null || player.location == null
                    || player.location.action == null || !player.IsTechnicallyAlive()
                    || !player.IsPlayerInSameRoomAndAction(location)) {
                continue;
            }
            ++players;
            if (lowest == null || player.id < lowest) {
                lowest = player.id;
            }
            generatorInBattle |= Objects.equals(generator, player.id);
        }
        return Objects.equals(P2PManager.selfID, generatorInBattle ? generator : lowest) ? players : 0;
    }

    static void count(AngerPower power, int quota) {
        if (quota < 1) {
            return;
        }
        int pending = PENDING.getOrDefault(power, 0) + 1;
        if (pending < quota) {
            PENDING.put(power, pending);
            return;
        }
        PENDING.put(power, pending - quota);
        AbstractDungeon.actionManager.addToTop(new GrantStrength(power));
    }

    private static final class GrantStrength extends AbstractGameAction {
        private final AngerPower anger;

        GrantStrength(AngerPower anger) {
            this.anger = anger;
            this.actionType = ActionType.POWER;
        }

        @Override
        public void update() {
            isDone = true;
            if (!isActive(anger)) {
                return;
            }
            // Queue the grant outside the card's iteration over the monster's powers.
            // Use the normal multiplayer power application and send it exactly once,
            // independent of the authority's local turn phase on packet arrival.
            anger.flash();
            StrengthPower strength = new StrengthPower(anger.owner, anger.amount);
            P2PCallbacks.OnMonsterPowerAdded(anger.owner, strength, null);
            P2PMessageSender.Send_MonsterPowerAdded(NetworkPower.Generate(strength));
        }
    }
}
