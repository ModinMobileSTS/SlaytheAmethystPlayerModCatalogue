package wizardrobe.relics;

import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.actions.common.HealAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import wizardrobe.WizardRobeMod;

public final class CompileDirective extends LobbyRareRelic {
    public static final String ID = WizardRobeMod.makeID("CompileDirective");
    private static final int BLOCK_PER_COMPILE = 3;
    private static final int COMPILES_PER_HEAL = 3;
    private static final int HEALS_PER_COMBAT = 3;

    // 本场战斗已完成的编译数，用于判断是否凑满 3 次；不计入存档，每场战斗重新计数。
    private int compilesThisCombat;

    public CompileDirective() {
        super(ID, "bronzeResources/images/relics/bottledCode.png",
                "bronzeResources/images/relics/outline/bottledCode.png");
        setCounter(HEALS_PER_COMBAT);
    }

    @Override
    public AbstractRelic makeCopy() {
        return new CompileDirective();
    }

    @Override
    public void atBattleStart() {
        // 每场战斗开始时重置；counter 会写进存档，读档后剩余次数依旧正确。
        compilesThisCombat = 0;
        setCounter(HEALS_PER_COMBAT);
    }

    @Override
    public void onVictory() {
        // 战斗结束后把右下角数字还原成 3，避免场间显示成上一场用剩的次数。
        setCounter(HEALS_PER_COMBAT);
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
        // 格挡不受每场战斗次数限制，每次编译都给。
        AbstractDungeon.actionManager.addToBottom(new GainBlockAction(player, BLOCK_PER_COMPILE));
        ++compilesThisCombat;
        if (compilesThisCombat % COMPILES_PER_HEAL != 0) {
            return;
        }
        // 每 3 次编译回 1 点生命，每场战斗最多 3 次；生命已满时留着次数不消耗。
        if (counter > 0 && counter <= HEALS_PER_COMBAT && player.currentHealth < player.maxHealth) {
            setCounter(counter - 1);
            AbstractDungeon.actionManager.addToBottom(new HealAction(player, player, 1));
        }
    }
}