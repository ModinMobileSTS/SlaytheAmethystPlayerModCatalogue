package wizardrobe.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import wizardrobe.WizardRobeMod;

public final class CompileDirective extends LobbyRareRelic {
    public static final String ID = WizardRobeMod.makeID("CompileDirective");
    private static final int BLOCK_PER_COMPILE = 3;

    public CompileDirective() {
        super(ID, "bronzeResources/images/relics/bottledCode.png",
                "bronzeResources/images/relics/outline/bottledCode.png");
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CompileDirective();
    }

    public void onFunctionCompiled(AbstractPlayer player) {
        if (player.isDead || player.isDying || player.isEscaping
                || AbstractDungeon.actionManager == null || AbstractDungeon.getCurrMapNode() == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.phase != AbstractRoom.RoomPhase.COMBAT) {
            return;
        }
        flash();
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction(player, BLOCK_PER_COMPILE));
    }
}
