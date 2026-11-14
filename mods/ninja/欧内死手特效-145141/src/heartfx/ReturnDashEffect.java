package heartfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 让冲出去的蕾忍停在前面，等目标的死亡动画播完再滑回原位。
 * 用特效（effectList）而不是动作队列实现，所以等待期间不会卡住其它动作。
 * 另外带三重保险：目标死亡动画结束 / 斩杀超时 / 战斗已结束，任一成立都会归位。
 */
public class ReturnDashEffect extends AbstractGameEffect {

    /** 斩首都还没发生时的最长等待。 */
    private static final float KILL_WAIT_TIMEOUT = 1.2f;
    /** 死亡动画（AbstractMonster.DEATH_TIME = 1.8s）播完后的兜底超时。 */
    private static final float DEATH_WAIT_TIMEOUT = 3.0f;
    private static final float SLIDE_DUR = 0.16f;

    private final AbstractCreature target;
    private final float homeAnimX;
    private float timer = 0.0f;
    private float slideTimer = 0.0f;
    private float fromAnimX = 0.0f;
    private boolean killSeen = false;
    private boolean sliding = false;

    public ReturnDashEffect(AbstractCreature target, float homeAnimX) {
        this.target = target;
        this.homeAnimX = homeAnimX;
        this.duration = 0.0f;
        this.startingDuration = 0.0f;
    }

    @Override
    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            this.isDone = true;
            return;
        }
        float dt = Gdx.graphics.getDeltaTime();

        if (this.sliding) {
            this.slideTimer += dt;
            float k = Math.min(1.0f, this.slideTimer / SLIDE_DUR);
            float eased = 1.0f - (1.0f - k) * (1.0f - k);
            p.animX = this.fromAnimX + (this.homeAnimX - this.fromAnimX) * eased;
            if (k >= 1.0f) {
                p.animX = this.homeAnimX;
                this.isDone = true;
            }
            return;
        }

        this.timer += dt;
        if (this.target != null && (this.target.isDying || this.target.currentHealth <= 0)) {
            this.killSeen = true;
        }

        boolean dead = this.target == null
                || this.target.isDead
                || this.target.isEscaping
                || (this.killSeen && !this.target.isDying);
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        boolean combatOver = room == null || room.phase != AbstractRoom.RoomPhase.COMBAT;
        boolean timeout = this.timer >= (this.killSeen ? DEATH_WAIT_TIMEOUT : KILL_WAIT_TIMEOUT);

        if (dead || combatOver || timeout) {
            this.sliding = true;
            this.slideTimer = 0.0f;
            this.fromAnimX = p.animX;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}
