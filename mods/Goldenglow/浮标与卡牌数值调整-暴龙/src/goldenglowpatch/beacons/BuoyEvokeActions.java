package goldenglowpatch.beacons;

import Goldenglow.helper.DroneManager;
import Goldenglow.helper.Hpr;
import Goldenglow.orbs.Buoy;
import Goldenglow.patches.DronePatch;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 激发指定浮标的两个小动作，供"信标的愤怒"重做后的效果使用。
 */
public final class BuoyEvokeActions {

    private BuoyEvokeActions() {
    }

    /** 激发玩家身上的一个指定浮标：先移到最左侧，再走原版的激发流程。 */
    public static final class PlayerBuoy extends AbstractGameAction {
        private final AbstractOrb orb;

        public PlayerBuoy(AbstractOrb orb) {
            this.orb = orb;
            this.duration = Settings.ACTION_DUR_FAST;
            this.actionType = ActionType.DAMAGE;
        }

        @Override
        public void update() {
            if (this.duration == Settings.ACTION_DUR_FAST) {
                AbstractPlayer p = AbstractDungeon.player;
                if (this.orb instanceof Buoy && p.orbs.contains(this.orb)) {
                    p.orbs.remove(this.orb);
                    p.orbs.add(0, this.orb);
                    p.evokeOrb();
                }
            }
            this.tickDuration();
        }
    }

    /** 激发某个怪物身上最靠左的一个浮标。 */
    public static final class MonsterBuoy extends AbstractGameAction {
        private final AbstractMonster target;

        public MonsterBuoy(AbstractMonster target) {
            this.target = target;
            this.duration = Settings.ACTION_DUR_FAST;
            this.actionType = ActionType.DAMAGE;
        }

        @Override
        public void update() {
            if (this.duration == Settings.ACTION_DUR_FAST && Hpr.isAlive(this.target)) {
                ArrayList<AbstractOrb> orbs = DronePatch.DroneFieldsPatch.orbs.get(this.target);
                int index = -1;
                for (int i = 0; i < orbs.size(); ++i) {
                    if (orbs.get(i) instanceof Buoy) {
                        index = i;
                        break;
                    }
                }
                if (index > 0) {
                    Collections.swap(orbs, 0, index);
                }
                if (index >= 0) {
                    DroneManager.evokeOrb(this.target);
                }
            }
            this.tickDuration();
        }
    }
}
