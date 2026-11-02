package TheNecrobinderMod.character;

// ============================================================
// 修复点（update 方法末段，标注 FIX 处）：
// 原版奥斯提死亡淡出结束后无条件调用 OstyManager.clear()，
// 若淡出期间（约0.5秒）玩家再次召唤，新奥斯提会被旧实例
// 的 clear() 从管理器中清掉（"刚召唤的奥斯提人间蒸发"）。
// 修复：只有当前管理器持有的实例是自己时才清空。
// 其余代码与原 class 反编译结果逐行一致，仅改动一行。
// ============================================================

import TheNecrobinderMod.BasicMod;
import TheNecrobinderMod.EventTracker;
import TheNecrobinderMod.OstyManager;
import TheNecrobinderMod.powers.buffs.NecroMasteryPower;
import TheNecrobinderMod.powers.buffs.OstyPower;
import TheNecrobinderMod.util.TextureLoader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.DamageAction;
import com.megacrit.cardcrawl.actions.common.RemoveSpecificPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Hitbox;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.vfx.combat.StrikeEffect;

public class Osty
        extends AbstractCreature {
    public static final String ID = BasicMod.makeID(Osty.class.getSimpleName());
    private static final float BASE_W = 110.0f;
    private static final float BASE_H = 130.0f;
    private final Texture img = TextureLoader.getTexture(BasicMod.imagePath("char/osty.png"));
    private float scale;
    private float targetScale;
    private float fade = 1.0f;

    public Osty(int hp) {
        this.id = ID;
        this.name = "Osty";
        this.isPlayer = false;
        this.maxHealth = this.currentHealth = hp;
        this.hbAlpha = 1.0f;
        this.hb = new Hitbox(110.0f * Settings.scale, 130.0f * Settings.scale);
        this.healthHb = new Hitbox(this.hb.width, 72.0f * Settings.scale);
        this.scale = this.targetScale = this.scaleForHealth();
        this.layout();
        this.healthBarUpdatedEvent();
    }

    private float scaleForHealth() {
        return 1.0f + 0.8f * (float) this.maxHealth / 100.0f;
    }

    private void layout() {
        AbstractPlayer p = AbstractDungeon.player;
        this.drawX = p.drawX + 220.0f * Settings.scale;
        this.drawY = p.drawY + 10.0f * Settings.scale;
        this.hb_w = 110.0f * Settings.scale * this.targetScale;
        this.hb_h = 130.0f * Settings.scale * this.targetScale;
        this.hb.resize(this.hb_w, this.hb_h);
        this.hb.move(this.drawX, this.drawY + this.hb_h / 2.0f);
        this.healthHb.resize(this.hb_w, 72.0f * Settings.scale);
        this.healthHb.move(this.hb.cX, this.hb.cY - this.hb_h / 2.0f - this.healthHb.height / 2.0f);
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
        AbstractPlayer p;
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
        }
        if ((p = AbstractDungeon.player).hasPower(NecroMasteryPower.POWER_ID)) {
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
        AbstractPlayer p = AbstractDungeon.player;
        if (p.hasPower(OstyPower.POWER_ID)) {
            AbstractDungeon.actionManager.addToTop((AbstractGameAction) new RemoveSpecificPowerAction(
                    (AbstractCreature) p, (AbstractCreature) p, OstyPower.POWER_ID));
        }
        AbstractDungeon.player.hand.applyPowers();
    }

    public boolean isDeadOrEscaped() {
        return this.isDying || this.currentHealth <= 0;
    }

    public void update() {
        float dt = Gdx.graphics.getDeltaTime();
        this.scale = MathUtils.lerp((float) this.scale, (float) this.targetScale, (float) (dt * 6.0f));
        this.layout();
        this.updateHealthBar();
        if (this.isDying) {
            this.fade -= dt * 2.0f;
            // FIX：淡出结束后，仅当管理器当前持有的仍是自己时才清空，
            // 防止淡出期间新召唤的奥斯提被误清。
            if (this.fade <= 0.0f && OstyManager.get() == this) {
                OstyManager.clear();
            }
        }
    }

    public void render(SpriteBatch spriteBatch) {
        spriteBatch.setColor(1.0f, 1.0f, 1.0f, Math.max(this.fade, 0.0f));
        float w = (float) this.img.getWidth() * Settings.scale * this.scale;
        float h = (float) this.img.getHeight() * Settings.scale * this.scale;
        spriteBatch.draw(this.img, this.drawX - w / 2.0f, this.drawY, w, h);
        if (!this.isDying) {
            this.renderHealth(spriteBatch);
        }
    }
}
