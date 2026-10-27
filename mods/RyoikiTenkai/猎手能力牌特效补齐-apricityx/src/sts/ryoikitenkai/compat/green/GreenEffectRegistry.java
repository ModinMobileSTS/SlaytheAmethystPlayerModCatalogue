package sts.ryoikitenkai.compat.green;

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
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.WeakHashMap;

/**
 * 猎手四张缺失能力牌的特效登记表与播放逻辑：精准、无限刀刃、工具贸易、蓄势待发。
 * <p>
 * 父模组的 powerImpls 表由 AutoAdd 只扫描 sts.ryoikitenkai.patch 包，扫不到本补丁的类，
 * 所以这里不依赖那张表：能力生命周期由本补丁自己的钩子驱动，按能力 ID 派发。
 * <p>
 * 所有「派生」特效（在别的特效 update() 里生成的）都必须走 {@link #queue}，
 * 也就是放进 effectsQueue；直接往 effectList 里加会在游戏遍历 effectList 时抛
 * ConcurrentModificationException。
 * <p>
 * 配色取猎手的绿色系：常驻用柔和的绿，触发用更亮的绿；贴图全部来自游戏自带资源。
 */
public final class GreenEffectRegistry {
    /** 能力 ID（与 {@code AbstractPower.ID} 完全一致）。 */
    public static final String ACCURACY_ID = "Accuracy";
    public static final String BLADES_ID = "Infinite Blades";
    public static final String TRADE_ID = "Tools Of The Trade";
    public static final String RETAIN_ID = "Retain Cards";

    /** 父模组设置面板里对应的卡牌 ID。 */
    public static final String ACCURACY_CARD = "Accuracy";
    public static final String BLADES_CARD = "Infinite Blades";
    public static final String TRADE_CARD = "Tools of the Trade";
    public static final String RETAIN_CARD = "Well Laid Plans";

    private static final Color GREEN = new Color(0.45f, 0.95f, 0.55f, 1.0f);
    private static final Color GREEN_BRIGHT = new Color(0.75f, 1.00f, 0.78f, 1.0f);

    private static final WeakHashMap<AbstractCreature, GlintsAuraEffect> ACCURACY_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, BladesAuraEffect> BLADES_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, CardFloatAuraEffect> TRADE_AURAS = new WeakHashMap<>();
    private static final WeakHashMap<AbstractCreature, CardFloatAuraEffect> RETAIN_AURAS = new WeakHashMap<>();

    private GreenEffectRegistry() {
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

    static Color silent() {
        return GREEN.cpy();
    }

    static Color silentBright() {
        return GREEN_BRIGHT.cpy();
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

    public static void startAccuracy(AbstractCreature owner) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        GlintsAuraEffect old = ACCURACY_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        GlintsAuraEffect aura = new GlintsAuraEffect(owner);
        ACCURACY_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopAccuracy(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        GlintsAuraEffect aura = ACCURACY_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startBlades(AbstractCreature owner) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        BladesAuraEffect old = BLADES_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        BladesAuraEffect aura = new BladesAuraEffect(owner);
        BLADES_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopBlades(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        BladesAuraEffect aura = BLADES_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startTrade(AbstractCreature owner) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        CardFloatAuraEffect old = TRADE_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        CardFloatAuraEffect aura = new CardFloatAuraEffect(owner, true);
        TRADE_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopTrade(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        CardFloatAuraEffect aura = TRADE_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    public static void startRetain(AbstractCreature owner) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        CardFloatAuraEffect old = RETAIN_AURAS.remove(owner);
        if (old != null) {
            old.isEnd = true;
        }
        CardFloatAuraEffect aura = new CardFloatAuraEffect(owner, false);
        RETAIN_AURAS.put(owner, aura);
        queue(aura);
    }

    public static void stopRetain(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        CardFloatAuraEffect aura = RETAIN_AURAS.remove(owner);
        if (aura != null) {
            aura.isEnd = true;
        }
    }

    // ---------------- 施放 ----------------

    /** 打出「精准」：绿色卡片轮廓闪光 + 脚下收束的绿光 + 几道向外掠出的刃光。 */
    public static void castAccuracy(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, gy + 5.0f * Settings.scale, hw * 1.55f, hw * 0.95f,
                0.26f, silent(), 0.42f, 8.5f * Settings.scale, 40, 0.00f, 0.44f));
        // 刃光：几道细长的绿线从身上向外掠出
        for (int i = 0; i < 4; ++i) {
            float angle = MathUtils.random(0.0f, MathUtils.PI2);
            float speed = MathUtils.random(180.0f, 300.0f) * Settings.scale;
            queue(new FlyingSpikeEffect(cx + MathUtils.random(-hw * 0.4f, hw * 0.4f),
                    chest + MathUtils.random(-0.10f, 0.16f) * BodyFx.height(owner),
                    angle * MathUtils.radiansToDegrees,
                    MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed, silentBright()));
        }
        spawnGlints(owner, 6, 1.10f);
    }

    /** 打出「无限刀刃」：绿色卡片轮廓闪光 + 一圈向外迸开的刀光（对应卡面上的飞刀）。 */
    public static void castInfiniteBlades(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, gy + 5.0f * Settings.scale, hw * 0.90f, hw * 1.75f,
                0.26f, silent(), 0.46f, 9.0f * Settings.scale, 44, 0.00f, 0.52f));
        for (int i = 0; i < 7; ++i) {
            float angle = MathUtils.PI2 * i / 7.0f + MathUtils.random(-0.25f, 0.25f);
            float speed = MathUtils.random(220.0f, 420.0f) * Settings.scale;
            queue(new FlyingSpikeEffect(cx, chest,
                    angle * MathUtils.radiansToDegrees,
                    MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed, silentBright()));
        }
        for (int i = 0; i < 5; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.85f),
                    gy + MathUtils.random(0.0f, 0.10f) * h,
                    h * MathUtils.random(0.60f, 0.95f),
                    MathUtils.random(6.0f, 10.0f) * Settings.scale, silent()));
        }
        spawnGlints(owner, 6, 1.20f);
    }

    /** 打出「工具贸易」：绿色卡片轮廓闪光 + 一抽一弃两张卡影 + 胸口一圈。 */
    public static void castToolsOfTheTrade(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, chest, hw * 0.25f, hw * 0.95f,
                0.92f, silentBright(), 0.32f, 7.0f * Settings.scale, 32, 0.00f, 0.46f));
        // 抽牌：从左下飘到胸口；弃牌：从右侧沉下去
        queue(new CardGlyphEffect(cx - hw * 0.80f, gy + h * 0.15f, hw * 0.55f, h * 0.55f,
                0.36f * Settings.scale, -8.0f, 14.0f, silentBright(), 0.85f, 0.80f));
        queue(new CardGlyphEffect(cx + hw * 0.85f, chest, hw * 0.30f, -h * 0.50f,
                0.34f * Settings.scale, 9.0f, -16.0f, silent(), 0.65f, 0.85f));
        spawnGlints(owner, 4, 1.00f);
    }

    /** 打出「蓄势待发」：绿色卡片轮廓闪光 + 一张被留下的卡影移到身侧。 */
    public static void castRetainCards(AbstractCreature owner, AbstractCard card) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        if (card != null) {
            queue(new CardFlashVfx(card, silent(), false));
        }
        queue(new RingPulseEffect(cx, chest, hw * 0.85f, hw * 0.35f,
                0.92f, silentBright(), 0.34f, 7.5f * Settings.scale, 32, 0.00f, 0.48f));
        queue(new CardGlyphEffect(cx, chest, hw * 0.78f, -h * 0.10f,
                0.40f * Settings.scale, 0.0f, 6.0f, silentBright(), 0.80f, 0.70f));
        for (int i = 0; i < 4; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.70f),
                    gy + MathUtils.random(0.10f, 0.30f) * h,
                    h * MathUtils.random(0.45f, 0.75f),
                    MathUtils.random(5.5f, 9.0f) * Settings.scale, silent()));
        }
        spawnGlints(owner, 3, 1.00f);
    }

    // ---------------- 触发 ----------------

    /** 使用小刀（精准生效）：刀锋附近闪一下绿光，证明这一下被「校准」过。 */
    public static void shivUsed(AbstractCreature owner) {
        if (owner == null || !isOn(ACCURACY_CARD)) {
            return;
        }
        if (owner.powers == null || !owner.hasPower(ACCURACY_ID)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float chest = BodyFx.chestY(owner);
        float hw = BodyFx.halfWidth(owner);

        queue(new RingPulseEffect(cx, chest, hw * 0.30f, hw * 0.72f,
                0.90f, silentBright(), 0.26f, 6.0f * Settings.scale, 26, 0.00f, 0.30f));
        for (int i = 0; i < 3; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.55f),
                    chest - BodyFx.height(owner) * 0.10f,
                    BodyFx.height(owner) * MathUtils.random(0.25f, 0.45f),
                    MathUtils.random(4.5f, 7.5f) * Settings.scale, silentBright()));
        }
        spawnGlints(owner, 4, 0.95f);
    }

    /** 回合开始获得小刀：一圈绿色刀光向四周散开。 */
    public static void bladeGained(AbstractCreature owner) {
        if (owner == null || !isOn(BLADES_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        queue(new RingPulseEffect(cx, gy + 4.0f * Settings.scale, hw * 0.80f, hw * 1.50f,
                0.26f, silent(), 0.36f, 8.0f * Settings.scale, 40, 0.00f, 0.44f));
        for (int i = 0; i < 5; ++i) {
            float angle = MathUtils.PI2 * i / 5.0f + MathUtils.random(-0.3f, 0.3f);
            float speed = MathUtils.random(200.0f, 380.0f) * Settings.scale;
            queue(new FlyingSpikeEffect(cx, chest,
                    angle * MathUtils.radiansToDegrees,
                    MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed, silentBright()));
        }
        for (int i = 0; i < 4; ++i) {
            queue(new RisingMoteEffect(BodyFx.bodyX(owner, 0.80f),
                    gy + MathUtils.random(0.0f, 0.08f) * h,
                    h * MathUtils.random(0.50f, 0.80f),
                    MathUtils.random(5.5f, 9.0f) * Settings.scale, silent()));
        }
        spawnGlints(owner, 5, 1.10f);
    }

    /** 回合开始抽一弃一：一升一降两张卡影。 */
    public static void toolsUpkeep(AbstractCreature owner) {
        if (owner == null || !isOn(TRADE_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float gy = BodyFx.groundY(owner);
        float hw = BodyFx.halfWidth(owner);
        float h = BodyFx.height(owner);
        float chest = BodyFx.chestY(owner);

        queue(new CardGlyphEffect(cx - hw * 0.75f, gy + h * 0.18f, hw * 0.45f, h * 0.50f,
                0.34f * Settings.scale, -7.0f, 12.0f, silentBright(), 0.75f, 0.75f));
        queue(new CardGlyphEffect(cx + hw * 0.80f, chest, hw * 0.25f, -h * 0.45f,
                0.32f * Settings.scale, 8.0f, -14.0f, silent(), 0.55f, 0.80f));
        queue(new RingPulseEffect(cx, chest, hw * 0.60f, hw * 0.95f,
                0.92f, silent(), 0.22f, 6.0f * Settings.scale, 26, 0.00f, 0.36f));
        spawnGlints(owner, 2, 0.95f);
    }

    /** 回合末保留卡牌：一张卡影被轻轻推到身侧。 */
    public static void retainTurn(AbstractCreature owner) {
        if (owner == null || !isOn(RETAIN_CARD)) {
            return;
        }
        float cx = BodyFx.centerX(owner);
        float hw = BodyFx.halfWidth(owner);
        float chest = BodyFx.chestY(owner);

        queue(new RingPulseEffect(cx, chest, hw * 0.75f, hw * 0.35f,
                0.92f, silentBright(), 0.28f, 7.0f * Settings.scale, 28, 0.00f, 0.36f));
        queue(new CardGlyphEffect(cx, chest, hw * 0.72f, -BodyFx.height(owner) * 0.08f,
                0.36f * Settings.scale, 0.0f, 5.0f, silentBright(), 0.70f, 0.60f));
        spawnGlints(owner, 2, 0.90f);
    }

    // ---------------- 共用的小批量派生 ----------------

    /**
     * 贴身绿色星芒：优先绑在骨架的骨头上，随动画形变移动；
     * 找不到合适的骨头时退回 hitbox 内的随机点，保证一定有反馈。
     */
    static void spawnGlints(AbstractCreature owner, int count, float sizeMul) {
        if (owner == null) {
            return;
        }
        Skeleton skeleton = BodyFx.skeleton(owner);
        Color color = silentBright();
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
        if (ACCURACY_ID.equals(power.ID)) {
            startAccuracy(power.owner);
        } else if (BLADES_ID.equals(power.ID)) {
            startBlades(power.owner);
        } else if (TRADE_ID.equals(power.ID)) {
            startTrade(power.owner);
        } else if (RETAIN_ID.equals(power.ID)) {
            startRetain(power.owner);
        }
    }

    /** 能力被移除/生物死亡时停掉常驻特效。 */
    public static void onPowerRemoved(AbstractPower power) {
        if (power == null || power.owner == null) {
            return;
        }
        if (ACCURACY_ID.equals(power.ID)) {
            stopAccuracy(power.owner);
        } else if (BLADES_ID.equals(power.ID)) {
            stopBlades(power.owner);
        } else if (TRADE_ID.equals(power.ID)) {
            stopTrade(power.owner);
        } else if (RETAIN_ID.equals(power.ID)) {
            stopRetain(power.owner);
        }
    }
}
