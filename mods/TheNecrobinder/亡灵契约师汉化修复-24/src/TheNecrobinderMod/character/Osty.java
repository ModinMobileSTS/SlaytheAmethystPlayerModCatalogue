package TheNecrobinderMod.character;

// ============================================================
// 基于父 mod 新版 Spine 动画 Osty 重写。
// 唯一修复点：update() 方法末段，淡出结束后仅当管理器
// 当前持有的仍是自己时才调 clear()，防止淡出期间（~0.5s）
// 新召唤的奥斯提被旧实例误清（"刚召唤的奥斯提人间蒸发"）。
// 其余代码与新版反编译结果一致。
// ============================================================

import TheNecrobinderMod.BasicMod;
import TheNecrobinderMod.EventTracker;
import TheNecrobinderMod.OstyManager;
import TheNecrobinderMod.powers.buffs.NecroMasteryPower;
import TheNecrobinderMod.powers.buffs.OstyPower;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.SkeletonMeshRenderer;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.StrikeEffect;

public class Osty
        extends AbstractCreature {
    public static final String ID = BasicMod.makeID(Osty.class.getSimpleName());
    private static final float BASE_W = 150.0f;
    private static final float BASE_H = 110.0f;
    private static final float BASE_SCALE = 1.25f;
    private static final float SCALE_PER_HP = 0.008f;
    private static final float BAR_DROP = 76.0f;
    private static final float BAR_SHIFT = 6.0f;
    private static final String ANIM_IDLE = "Idle";
    private static final String ANIM_ATTACK = "Attack";
    private static final String ANIM_POKE = "attack_poke";
    private static final String ANIM_HIT = "Hit";
    private static final String ANIM_DIE = "die";
    private static final String ANIM_SPAWN = "revive";
    private static final boolean PREMULTIPLIED = false;
    private static final SkeletonMeshRenderer renderer = new SkeletonMeshRenderer();
    private float scale;
    private float targetScale;
    private float fade = 1.0f;
    private final Color tint = new Color(1.0f, 1.0f, 1.0f, 1.0f);
    private float deathTimer = 0.0f;
    private float deathDuration = 1.2f;
    private long lastAttackFrame = -1L;

    public Osty(int hp) {
        this.id = ID;
        this.name = "Osty";
        this.isPlayer = false;
        this.maxHealth = this.currentHealth = hp;
        this.hbAlpha = 1.0f;
        this.hb = new Hitbox(150.0f * Settings.scale, 110.0f * Settings.scale);
        this.healthHb = new Hitbox(this.hb.width, 72.0f * Settings.scale);
        this.scale = this.targetScale = this.scaleForHealth();
        this.loadAnimation(BasicMod.imagePath("char/osty/osty.atlas"), BasicMod.imagePath("char/osty/osty.json"), 1.0f);
        this.stateData.setDefaultMix(0.05f);
        this.stateData.setMix(ANIM_ATTACK, ANIM_IDLE, 0.1f);
        this.stateData.setMix(ANIM_POKE, ANIM_IDLE, 0.1f);
        this.stateData.setMix(ANIM_HIT, ANIM_IDLE, 0.1f);
        this.stateData.setMix(ANIM_SPAWN, ANIM_IDLE, 0.1f);
        this.state.setAnimation(0, ANIM_SPAWN, false);
        AnimationState.TrackEntry idle = this.state.addAnimation(0, ANIM_IDLE, true, 0.0f);
        idle.setTime(idle.getEndTime() * MathUtils.random());
        this.layout();
        this.healthBarUpdatedEvent();
    }

    private float scaleForHealth() {
        return 1.25f + 0.008f * (float) this.maxHealth;
    }

    private void layout() {
        AbstractPlayer p = AbstractDungeon.player;
        this.drawX = p.drawX + 220.0f * Settings.scale;
        this.drawY = p.drawY + 10.0f * Settings.scale;
        float w = 150.0f * Settings.scale * this.targetScale;
        float h = 110.0f * Settings.scale * this.targetScale;
        float drop = 76.0f * Settings.scale;
        float shift = 6.0f * Settings.scale;
        this.hb_w = w + shift;
        this.hb_h = h + drop;
        this.hb.resize(this.hb_w, this.hb_h);
        this.hb.move(this.drawX + shift, this.drawY - drop + this.hb_h / 2.0f);
        this.healthHb.resize(w, 72.0f * Settings.scale);
        this.healthHb.move(this.hb.cX, this.hb.cY - this.hb_h / 2.0f - this.healthHb.height / 2.0f);
    }

    public void playAttack() {
        this.playAttack(false);
    }

    public void playAttack(boolean poke) {
        if (this.isDying) {
            return;
        }
        long frame = Gdx.graphics.getFrameId();
        if (frame == this.lastAttackFrame) {
            return;
        }
        this.lastAttackFrame = frame;
        this.state.setAnimation(0, poke ? ANIM_POKE : ANIM_ATTACK, false);
        this.state.addAnimation(0, ANIM_IDLE, true, 0.0f);
    }

    public void playHit() {
        if (this.isDying) {
            return;
        }
        this.state.setAnimation(0, ANIM_HIT, false);
        this.state.addAnimation(0, ANIM_IDLE, true, 0.0f);
    }

    public void gainMaxHP(int amount) {
        this.maxHealth += amount;
        this.currentHealth += amount;
        this.targetScale = this.scaleForHealth();
        this.layout();
        this.healthBarUpdatedEvent();
    }

    public void restore(int amount) {
        if (this.isDying || amount <= 0) {
            return;
        }
        this.currentHealth = Math.min(this.maxHealth, this.currentHealth + amount);
        this.healthBarUpdatedEvent();
    }

    private void loseHealth(int amount) {
        if (this.isDying || amount <= 0) {
            return;
        }
        int lost = this.currentHealth;
        this.currentHealth = Math.max(0, this.currentHealth - amount);
        lost -= this.currentHealth;
        AbstractDungeon.effectList.add(new StrikeEffect((AbstractCreature) this, this.hb.cX, this.hb.cY, amount));
        this.healthBarUpdatedEvent();
        if (this.currentHealth <= 0) {
            this.die();
        } else {
            this.playHit();
        }
        AbstractPlayer p = AbstractDungeon.player;
        if (p.hasPower(NecroMasteryPower.POWER_ID)) {
            int damage = lost * p.getPower((String) NecroMasteryPower.POWER_ID).amount;
            for (AbstractMonster m : AbstractDungeon.getMonsters().monsters) {
                AbstractDungeon.actionManager.addToBottom((AbstractGameAction) new DamageAction((AbstractCreature) m,
                        new DamageInfo((AbstractCreature) this, damage, DamageInfo.DamageType.THORNS),
                        AbstractGameAction.AttackEffect.NONE));
            }
        }
    }

    public int absorb(int amount) {
        int taken = Math.min(this.currentHealth, amount);
        this.loseHealth(taken);
        return amount - taken;
    }

    @Override
    public void damage(DamageInfo damageInfo) {
        int amount = Math.max(0, damageInfo.output);
        AbstractPlayer p = AbstractDungeon.player;
        if (damageInfo.type == DamageInfo.DamageType.NORMAL && p.currentBlock > 0) {
            int soaked = Math.min(p.currentBlock, amount);
            p.loseBlock(soaked);
            amount -= soaked;
        }
        this.loseHealth(amount);
    }

    public void die() {
        if (this.isDying) {
            return;
        }
        this.isDying = true;
        this.currentHealth = 0;
        EventTracker.addDeath();
        this.healthBarUpdatedEvent();
        AnimationState.TrackEntry death = this.state.setAnimation(0, ANIM_DIE, false);
        this.deathDuration = death.getEndTime();
        this.deathTimer = 0.0f;
        AbstractPlayer p = AbstractDungeon.player;
        if (p.hasPower(OstyPower.POWER_ID)) {
            AbstractDungeon.actionManager.addToTop((AbstractGameAction) new RemoveSpecificPowerAction(
                    (AbstractCreature) p, (AbstractCreature) p, OstyPower.POWER_ID));
        }
        AbstractDungeon.player.hand.applyPowers();
    }

    @Override
    public boolean isDeadOrEscaped() {
        return this.isDying || this.currentHealth <= 0;
    }

    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.scale = MathUtils.lerp(this.scale, this.targetScale, dt * 6.0f);
        this.layout();
        this.updateHealthBar();
        if (this.isDying) {
            this.deathTimer += dt;
            if (this.deathTimer > this.deathDuration) {
                this.fade -= dt * 2.0f;
                // FIX：淡出结束后，仅当管理器当前持有的仍是自己时才清空，
                // 防止淡出期间新召唤的奥斯提被误清。
                if (this.fade <= 0.0f && OstyManager.get() == this) {
                    OstyManager.clear();
                }
            }
        }
    }

    @Override
    public void render(SpriteBatch spriteBatch) {
        this.skeleton.setToSetupPose();
        this.state.update(Gdx.graphics.getDeltaTime());
        this.state.apply(this.skeleton);
        this.skeleton.getRootBone().setScale(this.scale);
        this.skeleton.setPosition(this.drawX, this.drawY);
        this.skeleton.updateWorldTransform();
        this.tint.set(1.0f, 1.0f, 1.0f, Math.max(this.fade, 0.0f));
        this.skeleton.setColor(this.tint);
        this.skeleton.setFlip(false, false);
        spriteBatch.end();
        CardCrawlGame.psb.begin();
        renderer.draw(CardCrawlGame.psb, this.skeleton);
        CardCrawlGame.psb.end();
        spriteBatch.begin();
        spriteBatch.setBlendFunction(770, 771);
        if (!this.isDying) {
            this.renderHealth(spriteBatch);
        }
    }

    static {
        renderer.setPremultipliedAlpha(false);
    }
}
