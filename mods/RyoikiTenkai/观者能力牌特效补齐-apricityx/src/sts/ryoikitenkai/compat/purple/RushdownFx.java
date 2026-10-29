package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.vfx.BorderFlashEffect;

import java.util.HashMap;

/**
 * 「猛虎下山」的规格、音效与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 整张牌只有一个生效点（已对照 {@code RushdownPower} 字节码核实）：
 * <pre>
 * public void onChangeStance(AbstractStance oldStance, AbstractStance newStance) {
 *     if (!oldStance.ID.equals(newStance.ID) &amp;&amp; newStance.ID.equals("Wrath")) {
 *         this.flash();
 *         this.addToBot(new DrawCardAction(this.owner, this.amount));
 *     }
 * }
 * </pre>
 * 即「<b>进入愤怒的那一瞬，抽牌</b>」。
 * <p>
 * <h2>抓痕只出现在敌人身上（本轮修正）</h2>
 * 玩家身上<b>不再有任何抓痕</b> —— 常驻抓痕与「掠过角色」的演出都已删除。
 * 理由：
 * <ul>
 *   <li>抓痕表达的是「这一刀撕到了谁」。画在自己身上，就与目标脱钩了</li>
 *   <li>观者另外三张牌的常驻（水面 / 五线谱 / 立方体）本来就占满角色周围，
 *       再往角色身上叠抓痕只会糊成一片</li>
 * </ul>
 * 所以这张牌现在的视觉只有一个落点：<b>敌人</b>。
 * <p>
 * <h2>小怪也用 boss 尺度（本轮修正）</h2>
 * 上一版抓痕长度取 {@code 目标判定框 × 1.15}，于是打小怪得到一枚小爪印、
 * 打 boss 得到一道大裂口 —— 同一种攻击读起来强度不同，小怪身上还根本看不清。
 * <p>
 * 现在改用<b>固定的大尺度</b>（{@link #CANVAS_RATIO} × 屏幕高度，约等于 boss 体型），
 * 所有敌人身上都是同一道醒目的裂口；锚点仍是目标判定框中心，
 * 所以照样「黏在它身上」。
 * <h2>反馈（这是「不爽」的真正原因）</h2>
 * <ul>
 *   <li>进入愤怒：低吼 + 轻震屏 + 一次边框闪光 —— 不画抓痕，
 *       反馈改由声音与画面震动承担</li>
 *   <li>愤怒中打攻击牌：只给 {@code ATTACK_DAGGER_1} 音效，<b>不震屏</b>
 *       （一回合可能打好几张，震屏会晕）</li>
 *   <li>施放这张牌：重震屏 + {@code THUNDERCLAP} + 边框闪光</li>
 * </ul>
 * 音效走 {@link #playOnce} 做 200ms 去重，连打不会糊成噪音。
 * <h2>空间签名（规范 §2.2）</h2>
 * 观者这套里唯一的「<b>落在敌人身上的斜向裂痕</b>」：
 * 另外三张牌都在角色身上或周围，只有猛虎把画面写到对面去。
 */
final class RushdownFx {
    /** 主色：取自卡面的橙金。 */
    static final Color FUR = new Color(1.00f, 0.62f, 0.24f, 1.0f);
    /** 暗部：抓痕底下的一层阴影，让裂痕在浅色立绘上也有对比。 */
    static final Color STRIPE = new Color(0.52f, 0.18f, 0.04f, 1.0f);
    /** 白热：抓痕内芯、刃口。 */
    static final Color CORE = new Color(1.00f, 0.95f, 0.84f, 1.0f);

    /**
     * 抓痕画布相对屏幕高度的比例。
     * <p>
     * 0.30 约等于一只 boss 的身高 —— 这样<b>小怪身上也是同一道大裂口</b>，
     * 同一张牌打出去，无论打谁，看到的都是同一种强度的爪击。
     * 若按目标判定框缩放，小怪身上会缩成看不见的小爪印。
     */
    static final float CANVAS_RATIO = 0.30f;

    /** 层数下限与上限（= 抽牌数，原版恒为 2）。 */
    static final int MIN_STACKS = 2;
    static final int MAX_STACKS = 6;

    private static final String SFX_LEAP = "RAGE";
    private static final String SFX_CLAW = "ATTACK_DAGGER_1";
    private static final String SFX_CAST = "THUNDERCLAP";

    /** 音效去重表：key → 上次播放的相对时刻。 */
    private static final HashMap<String, Float> LAST_AT = new HashMap<>();
    /** 去重窗口（秒）。 */
    private static final float DEDUP = 0.20f;

    private RushdownFx() {
    }

    // ---------------- 数值 ----------------

    /** 有效层数（抽牌数）：没有能力时按原版 2 兜底。 */
    static int stacks(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return MIN_STACKS;
        }
        AbstractPower power = owner.getPower(PurpleEffectRegistry.POWER_RUSHDOWN);
        if (power == null) {
            return MIN_STACKS;
        }
        return MathUtils.clamp(power.amount, MIN_STACKS, MAX_STACKS);
    }

    /** 强度 0~1：2 层 = 0.35，6 层 = 1.0。亮度与裂纹规模乘它。 */
    static float power(int stacks) {
        return MathUtils.lerp(0.35f, 1.0f,
                (MathUtils.clamp(stacks, MIN_STACKS, MAX_STACKS) - MIN_STACKS)
                        / (float) (MAX_STACKS - MIN_STACKS));
    }

    /** 是否处于愤怒。只有人能进愤怒。 */
    static boolean inWrath(AbstractCreature owner) {
        AbstractPlayer p = AbstractDungeon.player;
        return p != null
                && owner == p
                && p.stance != null
                && "Wrath".equals(p.stance.ID);
    }

    /** 固定的「boss 尺度」抓痕长度：统一用在所有敌人身上。 */
    static float canvasSize() {
        return Settings.HEIGHT * CANVAS_RATIO;
    }

    /** 这个敌人是否还该被画（活着、没逃、不是半死）。 */
    private static boolean alive(AbstractMonster m) {
        return m != null && !m.isDeadOrEscaped() && !m.halfDead;
    }

    /**
     * 非指向性、也没有明确目标时挑一个代表敌人，
     * 保证「打出攻击牌」永远有可见落点。
     */
    static AbstractMonster anyTarget(AbstractPlayer player) {
        if (AbstractDungeon.getMonsters() == null
                || AbstractDungeon.getMonsters().monsters == null) {
            return null;
        }
        AbstractMonster best = null;
        float bestDist = Float.MAX_VALUE;
        float px = player != null ? player.hb.cX : 0.0f;
        for (int i = 0; i < AbstractDungeon.getMonsters().monsters.size(); ++i) {
            AbstractMonster m = AbstractDungeon.getMonsters().monsters.get(i);
            if (!alive(m)) {
                continue;
            }
            float d = Math.abs(m.hb.cX - px);
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        return best;
    }

    // ---------------- 声音 ----------------

    /**
     * 带去重的单发音效。
     * <p>
     * 同一个 key 在 {@link #DEDUP} 秒内只响一次：愤怒期连打攻击牌时不会糊成噪音，
     * 但每张牌仍有明确的「爪」的听感。
     *
     * @param pitchVariation 音高扰动幅度（0.12 = ±12%），每次略有不同才不会机械
     */
    static void playOnce(String key, float pitchVariation) {
        if (CardCrawlGame.sound == null || key == null) {
            return;
        }
        float now = nowSeconds();
        Float last = LAST_AT.get(key);
        if (last != null && now - last < DEDUP) {
            return;
        }
        LAST_AT.put(key, now);
        CardCrawlGame.sound.play(key, pitchVariation);
    }

    /** 只用于 200ms 去重的相对秒表，不需要与游戏时间轴对齐。 */
    private static float nowSeconds() {
        return (float) (System.nanoTime() / 1_000_000_000.0);
    }

    // ---------------- 事件入口 ----------------

    /**
     * 进入愤怒：<b>不画任何抓痕</b>。
     * <p>
     * 反馈改由声音与画面震动承担：低吼 + 轻震屏 + 一次边框闪光。
     * 抓痕只留给真正落在敌人身上的那一刀。
     */
    static void onEnterWrath(AbstractPlayer player, int stacks) {
        if (player == null) {
            return;
        }
        PurpleEffectRegistry.queueTop(new BorderFlashEffect(FUR.cpy(), true));
        playOnce(SFX_LEAP, 0.12f);
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }

    /**
     * 愤怒中打出攻击牌：<b>把抓痕刻到这一刀实际打到的敌人身上</b>。
     * <p>
     * 分发规则：
     * <ol>
     *   <li>卡牌目标是 {@code ALL_ENEMY} / {@code ALL} → 每个活着的敌人各刻一套</li>
     *   <li>有明确单体目标 → 就刻在那一个身上</li>
     *   <li>以上都没有（{@code target} 为 null）→ 取最近的活敌人兜底</li>
     * </ol>
     * 所有敌人用的都是同一个 boss 尺度，所以打小怪一样醒目。
     *
     * @param card  打出的这张牌（用来读 {@code target} 形态）
     * @param heavy 是否重击（决定道数与粗细）
     */
    static void onWrathAttack(AbstractPlayer player, AbstractCard card,
                              AbstractMonster target, boolean heavy) {
        if (player == null) {
            return;
        }
        boolean hitAll = card != null
                && (card.target == AbstractCard.CardTarget.ALL_ENEMY
                || card.target == AbstractCard.CardTarget.ALL);

        if (hitAll) {
            // 群攻：每个活着的敌人身上都刻一套
            if (AbstractDungeon.getMonsters() != null
                    && AbstractDungeon.getMonsters().monsters != null) {
                for (int i = 0; i < AbstractDungeon.getMonsters().monsters.size(); ++i) {
                    AbstractMonster m = AbstractDungeon.getMonsters().monsters.get(i);
                    if (!alive(m)) {
                        continue;
                    }
                    PurpleEffectRegistry.queue(new RushdownClawEffect(player, m, heavy));
                }
            }
        } else {
            AbstractMonster aim = alive(target) ? target : anyTarget(player);
            if (aim != null) {
                PurpleEffectRegistry.queue(new RushdownClawEffect(player, aim, heavy));
            }
        }

        playOnce(SFX_CLAW, heavy ? 0.10f : 0.22f);
    }

    /** 施放这张牌本身：最重的一档。 */
    static void onCast(AbstractPlayer player, AbstractCard card) {
        if (player == null) {
            return;
        }
        // 边框闪光必须在顶层队列，否则会被手牌与 UI 压住
        PurpleEffectRegistry.queueTop(new BorderFlashEffect(FUR.cpy(), true));
        playOnce(SFX_CAST, 0.0f);
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.MED, ScreenShake.ShakeDur.SHORT, false);
        }
    }
}
