package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.cardManip.CardFlashVfx;
import com.megacrit.cardcrawl.vfx.combat.FlyingSpikeEffect;
import com.megacrit.cardcrawl.vfx.combat.ImpactSparkEffect;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.WeakHashMap;

/**
 * 「金属化」「进化」两个能力的特效登记表与播放逻辑。
 * <p>
 * 刻意不经过父模组的 powerImpls 表：父模组用 AutoAdd 只扫描 sts.ryoikitenkai.patch 包，
 * 扫不到本补丁的类。若把特效挂在那张表上，一旦手动注册没生效就会一个特效都不出现。
 * <p>
 * 所有「派生」特效（在别的特效 update() 里生成的）都必须走 {@link #queue}，
 * 也就是放进 effectsQueue；直接往 effectList 里加会在游戏遍历 effectList 时抛出
 * ConcurrentModificationException。
 * <p>
 * 配色取自卡面：金属化是冷钢蓝白，进化是荧光绿。两个能力都用「身体上/脚下」的表现，
 * 不画魔法阵、不动角色着色器 —— 原版特效本来就是这些贴图与加色光点。
 */
public final class RedEffectRegistry {
    public static final String EVOLVE_ID = "Evolve";
    public static final String METALLICIZE_ID = "Metallicize";

    /** 金属化主色：偏冷的钢蓝，对应卡面与 armor 能力图标。 */
    private static final Color STEEL = new Color(0.70f, 0.87f, 1.00f, 1.0f);
    /** 金属化最亮的一层：星芒与斜掠高光。 */
    private static final Color STEEL_BRIGHT = new Color(0.92f, 0.98f, 1.00f, 1.0f);
    /** 进化主色：卡面里的荧光绿。 */
    private static final Color BIO = new Color(0.40f, 1.00f, 0.60f, 1.0f);
    /** 进化最亮的一层。 */
    private static final Color BIO_BRIGHT = new Color(0.74f, 1.00f, 0.82f, 1.0f);

    private static final WeakHashMap<AbstractCreature, MetalAuraEffect> METAL_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, EvolveAuraEffect> BIO_AURAS = new WeakHashMap<>();

    private RedEffectRegistry() {
    }

    /** 能力是否由玩家开关打开（与父模组设置面板共用同一份开关）。 */
    public static boolean enabled(String cardId) {
        return RyoikiTenkai.isEnable(cardId);
    }

    static boolean isOn(String cardId) {
        return enabled(cardId);
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

    static Color steel() {
        return STEEL.cpy();
    }

    static Color steelBright() {
        return STEEL_BRIGHT.cpy();
    }

    static Color bio() {
        return BIO.cpy();
    }

    static Color bioBright() {
        return BIO_BRIGHT.cpy();
    }

    /** 能力层数；没有这个能力时按 1 处理，避免除零与空指针。 */
    static int stacksOf(AbstractCreature owner, String powerId) {
        if (owner == null || owner.powers == null) {
            return 1;
        }
        AbstractPower power = owner.getPower(powerId);
        return power == null ? 1 : Math.max(1, Math.abs(power.amount));
    }

    // ---------------- 常驻光效 ----------------

    public static void startMetallicize(AbstractCreature owner) {
        if (owner == null || !isOn(METALLICIZE_ID)) {
            return;
        }
        MetalAuraEffect old = METAL_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        MetalAuraEffect aura = new MetalAuraEffect(owner);
        METAL_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopMetallicize(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        MetalAuraEffect aura = METAL_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startEvolve(AbstractCreature owner) {
        if (owner == null || !isOn(EVOLVE_ID)) {
            return;
        }
        EvolveAuraEffect old = BIO_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        EvolveAuraEffect aura = new EvolveAuraEffect(owner);
        BIO_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopEvolve(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        EvolveAuraEffect aura = BIO_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    // ---------------- 金属化 ----------------

    /**
     * 打出「金属化」的瞬间。
     * <p>
     * 卡面闪着钢蓝的轮廓（{@link CardFlashVfx} 用的是卡牌自己的背景剪影，原版升级/触发都在用），
     * 同时脚下护甲合拢、三道斜光扫过甲面、火花与星芒迸开。
     */
    public static void castMetallicize(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(METALLICIZE_ID)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, steel(), false));
        }
        // 护甲在脚下合拢：两圈由外向内收束
        queue(new RingPulseEffect(cx, gy + 5.0f * Settings.scale, hw * 1.85f, hw * 1.02f,
                0.26f, steel(), 0.55f, 9.0f * Settings.scale, 46, 0.00f, 0.50f));
        queue(new RingPulseEffect(cx, gy + 5.0f * Settings.scale, hw * 1.50f, hw * 0.92f,
                0.26f, steelBright(), 0.40f, 7.0f * Settings.scale, 40, 0.10f, 0.44f));
        // 三道斜掠的金属高光扫过甲面
        for (int i = 0; i < 3; ++i) {
            queue(new SheenEffect(cx, gy, hw, h, steelBright(), 0.09f * i));
        }
        spawnSparks(owner, 20);
        spawnGlints(owner, 4, 1.45f, steelBright());
    }

    /**
     * 回合末靠金属化获得格挡。
     * <p>
     * 比施放克制：一圈由外向内收束的光环（护甲合拢）+ 一道斜光 + 少量火花，
     * 和原版 {@code GainBlockAction} 的格挡数字是同一拍子。
     */
    public static void blockMetallicize(AbstractCreature owner) {
        if (owner == null || !isOn(METALLICIZE_ID)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);

        queue(new RingPulseEffect(cx, gy + 4.0f * Settings.scale, hw * 1.62f, hw * 1.00f,
                0.26f, steel(), 0.46f, 8.0f * Settings.scale, 42, 0.00f, 0.42f));
        queue(new SheenEffect(cx, gy, hw, BodyFx.height(owner), steelBright(), 0.0f));
        spawnSparks(owner, 9);
        spawnGlints(owner, 2, 1.10f, steelBright());
    }

    // ---------------- 进化 ----------------

    /**
     * 打出「进化」的瞬间。
     * <p>
     * 绿色轮廓的卡片闪光 + 脚下扩散的绿色能量环 + 从脚底涌到头顶的光点与向上冲的绿色光条，
     * 呼应卡面上那股向上生长的能量。
     */
    public static void castEvolve(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(EVOLVE_ID)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, bio(), false));
        }
        // 脚下能量向外扩散
        queue(new RingPulseEffect(cx, gy + 5.0f * Settings.scale, hw * 0.85f, hw * 1.75f,
                0.26f, bio(), 0.50f, 9.0f * Settings.scale, 46, 0.00f, 0.55f));
        // 胸口一圈，像心跳一样顶开
        queue(new RingPulseEffect(cx, chest, hw * 0.20f, hw * 1.05f,
                0.92f, bioBright(), 0.34f, 7.5f * Settings.scale, 34, 0.06f, 0.48f));
        // 从脚底向头顶涌起的能量
        for (int i = 0; i < 14; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.90f),
                    gy + MathUtils.random(0.0f, 0.12f) * h,
                    h * MathUtils.random(0.85f, 1.15f),
                    MathUtils.random(7.0f, 12.0f) * Settings.scale, bio()));
        }
        // 向上冲的光条（原版 PowerBuffEffect 同款贴图，方向朝上）
        for (int i = 0; i < 7; ++i) {
            queue(new FlyingSpikeEffect(BodyFx.bodyX(owner, 0.55f),
                    gy + h * MathUtils.random(0.05f, 0.45f),
                    90.0f, 0.0f,
                    MathUtils.random(220.0f, 460.0f) * Settings.scale, bio()));
        }
        spawnGlints(owner, 4, 1.30f, bioBright());
    }

    /**
     * 抽到状态牌、进化触发额外抽牌。
     * <p>
     * 比施放轻：胸口一圈能量 + 一簇上冲光点与光条，读起来是「被状态牌顶了一下，能量往上顶」。
     */
    public static void drawEvolve(AbstractCreature owner) {
        if (owner == null || !isOn(EVOLVE_ID)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        queue(new RingPulseEffect(cx, chest, hw * 0.28f, hw * 0.98f,
                0.94f, bio(), 0.38f, 7.0f * Settings.scale, 32, 0.00f, 0.42f));
        for (int i = 0; i < 7; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.85f),
                    gy + MathUtils.random(0.15f, 0.45f) * h,
                    h * MathUtils.random(0.45f, 0.80f),
                    MathUtils.random(6.0f, 10.0f) * Settings.scale, bio()));
        }
        for (int i = 0; i < 4; ++i) {
            queue(new FlyingSpikeEffect(BodyFx.bodyX(owner, 0.50f),
                    chest + MathUtils.random(-10.0f, 20.0f) * Settings.scale,
                    90.0f, 0.0f,
                    MathUtils.random(200.0f, 380.0f) * Settings.scale, bio()));
        }
        spawnGlints(owner, 2, 1.00f, bioBright());
    }

    // ---------------- 共用的小批量派生 ----------------

    /** 常驻光环每隔一小段时间闪一下的金属反光。 */
    static void spawnMetalGlint(AbstractCreature owner) {
        spawnGlints(owner, 1, 1.00f, steelBright());
    }

    /** 常驻光环里的绿色星芒。 */
    static void spawnBioGlint(AbstractCreature owner) {
        spawnGlints(owner, 1, 1.15f, bioBright());
    }

    /** 常驻光环里的一颗上升光点。 */
    static void spawnBioMote(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        float h = BodyFx.height(owner);
        queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.85f),
                BodyFx.groundY(owner) + MathUtils.random(0.0f, 0.06f) * h,
                h * MathUtils.random(0.70f, 1.00f),
                MathUtils.random(5.5f, 9.5f) * Settings.scale, bio()));
    }

    /**
     * 贴身的金属火花。
     * <p>
     * 用原版 {@link ImpactSparkEffect}（白色、带重力、会在地面弹跳），
     * 比自绘粒子更像原版打击/格挡时的碎屑。
     */
    private static void spawnSparks(AbstractCreature owner, int count) {
        for (int i = 0; i < count; ++i) {
            queue(new ImpactSparkEffect(BodyFx.bodyX(owner, 0.72f), BodyFx.bodyY(owner, 0.24f, 0.82f)));
        }
    }

    /**
     * 贴身的星芒：优先绑在骨架的骨头上，随动画形变移动；
     * 找不到合适的骨头时退回 hitbox 内的随机点，保证一定有反馈。
     */
    private static void spawnGlints(AbstractCreature owner, int count, float sizeMul, Color color) {
        if (owner == null) {
            return;
        }
        Skeleton skeleton = BodyFx.skeleton(owner);
        float base = Math.max(9.0f, 13.0f * Settings.scale) * Math.max(0.1f, sizeMul);
        for (int i = 0; i < count; ++i) {
            float size = base * MathUtils.random(0.72f, 1.28f);
            Bone bone = BodyFx.randomBodyBone(skeleton, owner);
            if (bone != null) {
                queue(new GlintEffect(skeleton, bone, 0.0f, 0.0f, color, size));
            } else {
                queue(new GlintEffect(null, null,
                        BodyFx.bodyX(owner, 0.80f), BodyFx.bodyY(owner, 0.30f, 0.85f),
                        color, size));
            }
        }
    }

    // ---------------- 驱动 ----------------

    /** 能力刚被施加时启动常驻特效。 */
    public static void onPowerApplied(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        if (EVOLVE_ID.equals(power.ID)) {
            startEvolve(power.owner);
        } else if (METALLICIZE_ID.equals(power.ID)) {
            startMetallicize(power.owner);
        }
    }

    /** 能力被移除/生物死亡时停掉常驻特效。 */
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
