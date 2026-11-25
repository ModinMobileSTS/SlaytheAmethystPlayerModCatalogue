package wizardrobe.relics;

import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.watcher.VigorPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import wizardrobe.WizardRobeMod;

public final class PastLifeMemory extends LobbyRareRelic {
    public static final String ID = WizardRobeMod.makeID("PastLifeMemory");
    private static final int VIGOR_PER_ENCODE = 1;
    private static final int VIGOR_PER_FUNCTION = 3;

    public PastLifeMemory() {
        super(ID, "PastLifeMemory");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new PastLifeMemory();
    }

    public void onCardEncoded() {
        gainVigor(VIGOR_PER_ENCODE);
    }

    public void onFunctionPlayed() {
        gainVigor(VIGOR_PER_FUNCTION);
    }

    private void gainVigor(int amount) {
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null || player.currentHealth <= 0 || player.isDead || player.isEscaping
                || player.getRelic(ID) != this || AbstractDungeon.actionManager == null
                || AbstractDungeon.getCurrMapNode() == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.phase != AbstractRoom.RoomPhase.COMBAT || room.monsters == null
                || room.smoked || room.isBattleEnding()) {
            return;
        }
        flash();
        // 排在这次出牌已经排好的结算之后，避免攻击消耗刚获得的活力。
        AbstractDungeon.actionManager.addToBottom(
                new ApplyPowerAction(player, player, new VigorPower(player, amount), amount));
    }
}
