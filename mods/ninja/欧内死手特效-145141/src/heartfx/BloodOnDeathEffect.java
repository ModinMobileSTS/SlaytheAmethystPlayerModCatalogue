package heartfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;

/**
 * 血液飞溅的「扳机」：演出结束时不直接喷血，而是每帧盯着目标，
 * 等它真正进入死亡状态的那一帧才生成血液飞溅，保证血花正好在敌人死掉的瞬间炸开。
 * 生成时把目标的模型体积（碰撞框宽高）传进去，血量多少随体型等比例放大。
 */
public class BloodOnDeathEffect extends AbstractGameEffect {

    private static final float TIMEOUT = 3.0f;

    private final AbstractCreature target;
    private float timer = 0.0f;

    public BloodOnDeathEffect(AbstractCreature target) {
        this.target = target;
        this.duration = TIMEOUT;
        this.startingDuration = TIMEOUT;
        this.renderBehind = true;
    }

    @Override
    public void update() {
        this.timer += Gdx.graphics.getDeltaTime();

        if (this.target != null
                && (this.target.isDying || this.target.isDead || this.target.currentHealth <= 0)) {
            float bodyW = this.target.hb.width;
            float bodyH = this.target.hb.height;
            // 在特效遍历中，必须走队列，不能直接改 effectList
            AbstractDungeon.effectsQueue.add(new BloodSplatterEffect(
                    this.target.hb.cX, this.target.hb.cY,
                    this.target.drawY + 10.0f * Settings.scale,
                    bodyW, bodyH));
            this.isDone = true;
            return;
        }

        if (this.timer >= TIMEOUT) {
            this.isDone = true;
        }
    }

    @Override
    public void render(SpriteBatch sb) {
    }

    @Override
    public void dispose() {
    }
}
