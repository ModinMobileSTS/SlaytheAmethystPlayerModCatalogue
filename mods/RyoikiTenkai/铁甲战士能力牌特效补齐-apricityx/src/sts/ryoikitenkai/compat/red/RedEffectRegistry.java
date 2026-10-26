package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.combat.ImpactSparkEffect;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.WeakHashMap;

/**
 * 两个能力的特效登记表与播放逻辑。
 * <p>
 * 刻意不经过父模组的 powerImpls 表：父模组用 AutoAdd 只扫描 sts.ryoikitenkai.patch 包，
 * 扫不到本补丁的类。若把特效挂在那张表上，一旦手动注册没生效就会一个特效都不出现。
 * <p>
 * 所有「派生」特效（在别的特效 update() 里生成的）都必须走 {@link #queue}，
 * 也就是放进 effectsQueue；直接往 effectList 里加会在游戏遍历 effectList 时抛出
 * ConcurrentModificationException。
 * <p>
 * 金属化的主体是渲染角色时的剪影描边（见 {@link ArmorSilhouettePatch}），
 * 这里只负责判断是否需要描边，以及施放/格挡时的火星反馈与增亮冲量。
 */
public final class RedEffectRegistry {
    public static final String EVOLVE_ID = "Evolve";
    public static final String METALLICIZE_ID = "Metallicize";

    private static final WeakHashMap<AbstractCreature, EvolveMagicCircle> CIRCLES = new WeakHashMap<>();
    /** 金属化的施放冲量：>0 时描边额外增亮并更外扩，随时间衰减。 */
    private static float metallicizeFlash = 0.0f;

    private RedEffectRegistry() {
    }

    /** 能力是否由玩家开关打开（与父模组设置面板共用同一份开关）。 */
    public static boolean enabled(String cardId) {
        return RyoikiTenkai.isEnable(cardId);
    }

    private static boolean isTracked(String powerId) {
        return EVOLVE_ID.equals(powerId) || METALLICIZE_ID.equals(powerId);
    }

    /**
     * 安全入队：放进 effectsQueue。
     * 游戏的 effectsQueue 会在遍历 effectList 之外被结算，因此在特效的 update() 里调用它是安全的。
     */
    public static void queue(AbstractGameEffect effect) {
        if (effect != null) {
            AbstractDungeon.effectsQueue.add(effect);
        }
    }

    /** 阵半径，供魔法阵与粒子共用，保证粒子从阵的边缘冒出。 */
    public static float circleRadius(AbstractCreature owner) {
        float w = owner != null ? owner.hb.width : 120.0f * Settings.scale;
        return Math.max(78.0f * Settings.scale, w * 0.94f);
    }

    // ---------------- 进化 ----------------

    public static void startEvolve(AbstractCreature owner) {
        if (owner == null || CIRCLES.containsKey(owner)) {
            return;
        }
        EvolveMagicCircle circle = new EvolveMagicCircle(owner);
        CIRCLES.put(owner, circle);
        queue(circle);
    }

    public static void stopEvolve(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        EvolveMagicCircle circle = CIRCLES.remove(owner);
        if (circle != null) {
            circle.isEnd = true;
        }
    }

    /**
     * 魔法阵呼吸上升相涌出的 DNA 粒子：出生点落在外圈环上，
     * 这样读起来就是「阵一呼吸就冒出 DNA」。
     * 由 EvolveMagicCircle.update() 调用，只能走 effectsQueue。
     */
    public static void spawnMagicCircleHelix(AbstractCreature owner) {
        if (owner == null || !enabled(EVOLVE_ID)) {
            return;
        }
        if (!CIRCLES.containsKey(owner)) {
            return;
        }
        float radius = circleRadius(owner);
        float ang = MathUtils.random(0.0f, MathUtils.PI2);
        float rr = radius * MathUtils.random(0.55f, 0.98f);
        queue(EvolveHelix.periodic(owner, rr * MathUtils.cos(ang)));
    }

    public static void castEvolve(AbstractCreature owner) {
        if (owner == null || !enabled(EVOLVE_ID)) {
            return;
        }
        float base = circleRadius(owner);
        // 从阵心连续扩散三圈
        for (int i = 0; i < 3; ++i) {
            queue(new EvolveGroundRing(owner,
                    base * 0.30f, base * (1.9f + 0.42f * i),
                    0.80f + 0.10f * i, 0.95f - 0.14f * i,
                    Math.max(3.6f, 6.4f * Settings.scale) - 0.7f * i,
                    i * 0.11f));
        }
        // 同时涌出一批更高的双链
        for (int i = 0; i < 3; ++i) {
            queue(EvolveHelix.burst(owner, i));
        }
    }

    public static void drawEvolve(AbstractCreature owner) {
        if (owner == null || !enabled(EVOLVE_ID)) {
            return;
        }
        float base = circleRadius(owner);
        queue(new EvolveGroundRing(owner,
                base * 0.50f, base * 1.70f, 0.58f, 0.88f,
                Math.max(3.0f, 5.2f * Settings.scale)));
        for (int i = 0; i < 2; ++i) {
            queue(EvolveHelix.burst(owner, i));
        }
    }

    // ---------------- 金属化 ----------------

    /** 描边是否应该出现在这个玩家身上（渲染期每帧调用，必须轻量）。 */
    public static boolean shouldOutline(AbstractPlayer player) {
        if (player == null || player.powers == null) {
            return false;
        }
        if (!enabled(METALLICIZE_ID)) {
            return false;
        }
        return player.hasPower(METALLICIZE_ID);
    }

    /** 施放/格挡时的额外增亮强度，0~1，供描边读取。 */
    public static float metallicizeFlash() {
        return metallicizeFlash;
    }

    /** 每帧衰减，由描边补丁在绘制前调用一次。 */
    public static void tickMetallicizeFlash(float dt) {
        if (metallicizeFlash > 0.0f) {
            metallicizeFlash = Math.max(0.0f, metallicizeFlash - dt * 2.4f);
        }
    }

    public static void startMetallicize(AbstractCreature owner) {
        // 描边是渲染期按需绘制的，不需要常驻对象
    }

    public static void stopMetallicize(AbstractCreature owner) {
        if (owner instanceof AbstractPlayer) {
            metallicizeFlash = 0.0f;
        }
    }

    public static void castMetallicize(AbstractCreature owner) {
        if (owner == null || !enabled(METALLICIZE_ID)) {
            return;
        }
        metallicizeFlash = 1.0f;
        for (int i = 0; i < 18; ++i) {
            queue(new ImpactSparkEffect(owner.hb.cX, owner.hb.cY));
        }
    }

    public static void blockMetallicize(AbstractCreature owner) {
        if (owner == null || !enabled(METALLICIZE_ID)) {
            return;
        }
        metallicizeFlash = 0.75f;
        for (int i = 0; i < 10; ++i) {
            queue(new ImpactSparkEffect(owner.hb.cX, owner.hb.y + 20.0f));
        }
    }

    // ---------------- 驱动 ----------------

    /** 能力刚被施加时启动常驻特效。 */
    public static void onPowerApplied(AbstractPower power) {
        if (power == null || power.owner == null || !isTracked(power.ID)) {
            return;
        }
        if (!enabled(power.ID)) {
            return;
        }
        if (EVOLVE_ID.equals(power.ID)) {
            startEvolve(power.owner);
        } else {
            startMetallicize(power.owner);
        }
    }

    public static void onPowerRemoved(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        if (EVOLVE_ID.equals(power.ID)) {
            stopEvolve(power.owner);
        } else if (METALLICIZE_ID.equals(power.ID)) {
            stopMetallicize(power.owner);
        }
    }
}
