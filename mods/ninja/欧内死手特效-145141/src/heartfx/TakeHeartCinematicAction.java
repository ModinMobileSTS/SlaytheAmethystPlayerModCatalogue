package heartfx;

import com.badlogic.gdx.Gdx;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;

/**
 * 「我夺取心脏」专属演出：
 * 1. 略一停顿（蓄势），蕾忍纸片人突然向前平移，冲出约双方距离的 40%（只动 animX，不动判定框）；
 * 2. 同一瞬间：破空音效 + 震屏 + 身后的暗红速度线 + 一串红色拖影；
 * 3. 动作结束后交还原版斩杀流程，敌人在死亡动画开始的瞬间才炸开血液飞溅、并在地上留下 2D 血迹；
 * 4. 蕾忍保持前压姿态直到目标死亡动画播完，再滑回原位。
 */
public class TakeHeartCinematicAction extends AbstractGameAction {

    /** 整段动画 0.30 秒，突进落在 80% 处。 */
    private static final float DASH_AT = 0.24f;
    private static final float DASH_DUR = 0.06f;
    private static final float END_AT = 0.30f;

    private final AbstractCreature target;
    private float elapsed = 0.0f;
    private boolean started = false;
    private boolean dashDone = false;
    private boolean handoff = false;
    private float baseAnimX = 0.0f;
    private float dashDist = 0.0f;

    public TakeHeartCinematicAction(AbstractCreature target) {
        this.target = target;
        this.actionType = AbstractGameAction.ActionType.SPECIAL;
        this.duration = END_AT;
        this.startDuration = END_AT;
    }

    @Override
    public void update() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null) {
            this.isDone = true;
            return;
        }
        if (!this.started) {
            this.started = true;
            this.baseAnimX = p.animX;
            this.dashDist = HeartFx.dashDistance(p, this.target);
        }
        this.elapsed += Gdx.graphics.getDeltaTime();

        // 突进：音效、速度线、拖影全部跟着这一刻
        if (this.elapsed >= DASH_AT) {
            if (!this.dashDone) {
                this.dashDone = true;
                CardCrawlGame.sound.play(HeartFx.SFX_DASH);
                CardCrawlGame.screenShake.shake(ScreenShake.ShakeIntensity.MED,
                        ScreenShake.ShakeDur.SHORT, false);
                AbstractDungeon.effectList.add(new DashSpeedLinesEffect(p, this.dashDist));
                if (NinjaAfterimageEffect.canUse(p)) {
                    AbstractDungeon.effectList.add(new NinjaAfterimageEffect(p, this.dashDist));
                }
            }
            float k = Math.min(1.0f, (this.elapsed - DASH_AT) / DASH_DUR);
            float eased = 1.0f - (1.0f - k) * (1.0f - k) * (1.0f - k);
            p.animX = this.baseAnimX + this.dashDist * eased;
        }

        // 交还给斩杀流程：挂上「死亡瞬间才喷血」的扳机与归位登记
        if (this.elapsed >= END_AT) {
            if (!this.handoff) {
                this.handoff = true;
                if (this.target != null) {
                    AbstractDungeon.effectList.add(new BloodOnDeathEffect(this.target));
                }
                AbstractDungeon.effectList.add(new ReturnDashEffect(this.target, this.baseAnimX));
            }
            this.isDone = true;
        }
    }
}
