package potionfix;

import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

/**
 * 混乱（ChaosPower）真正生效所需的“重新决策”逻辑。
 * 原版 AbstractMonster.rollMove() 只调用 getMove()，要真正刷新意图显示与 intentDmg
 * 必须在 rollMove() 之后再调用 createIntent()。
 */
final class ChaosReroll {
    private ChaosReroll() {
    }

    static void reroll(AbstractCreature owner) {
        if (!(owner instanceof AbstractMonster)) {
            return;
        }
        AbstractMonster monster = (AbstractMonster) owner;
        if (monster.isDead || monster.isDying || monster.halfDead || monster.escaped || monster.isEscaping) {
            return;
        }
        try {
            monster.rollMove();
            monster.createIntent();
        } catch (Throwable ignored) {
            // 少数敌人在特殊状态下无法重新决策，这里忽略以免中断整场战斗
        }
    }
}
