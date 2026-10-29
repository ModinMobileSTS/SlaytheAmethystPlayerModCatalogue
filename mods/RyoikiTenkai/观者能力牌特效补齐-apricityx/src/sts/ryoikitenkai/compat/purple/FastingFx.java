package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.OverlayMenu;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ScreenShake;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.ui.panels.EnergyPanel;

import java.util.HashMap;

/**
 * 「斋戒」（Fasting2）的规格、几何、配色、音效与事件入口。
 * <p>
 * <h2>机制（规范 §2.3：视觉来自机制）</h2>
 * 已对照字节码核实。这张牌是全观者唯一一张<b>双向交换</b>的牌：
 * <pre>
 * // Fasting.use()  ——  一次性的「得」
 * VFXAction(new FastingEffect(p.hb.cX, p.hb.cY, Color.CHARTREUSE))
 * ApplyPowerAction(StrengthPower,  +magicNumber)    // +3 力量（升级 +4）
 * ApplyPowerAction(DexterityPower, +magicNumber)    // +3 敏捷
 * ApplyPowerAction(EnergyDownPower(p, 1, true), 1)  // 每回合 -1 能量
 *
 * // EnergyDownPower.atStartOfTurn()  ——  持续的「失」
 * addToBot(new LoseEnergyAction(amount));
 * </pre>
 * 注意原版自己就会放一圈 {@code FastingEffect}（黄绿），
 * 所以本补丁的「光圈」全部<b>复用同一个类</b>（见 {@link FastingDraw#vanillaRing}），
 * 保证与原版观感一致。
 * <h2>设计：把因果链画成一条路径</h2>
 * <pre>
 *   回合开始（失）：① 能量槽抽出能量球 → ② 加速飞向观者 →
 *                  ③ 命中后原版光圈：先红（力量）后绿（敏捷）
 *   施放（得）：    ① 脚下起势 → ② 红绿能量球串涌上胸口 →
 *                  ③ 原版光圈：先红后绿
 * </pre>
 * <h2>球的颜色跟随能量</h2>
 * 球体贴图直接取 {@code AbstractPlayer.getEnergyImage()} ——
 * 观者是 {@code PURPLE_ORB_FLASH_VFX}（紫）、战士红、猎手绿、机器人蓝。
 * 所以球的颜色<b>自动</b>与角色能量槽一致。
 * <h2>红圈先、绿圈后</h2>
 * 原版 {@code Fasting.use()} 的施加顺序就是先 {@code StrengthPower}（力量 → 红）
 * 后 {@code DexterityPower}（敏捷 → 绿）。<b>画面顺序与代码顺序一致</b>。
 * <h2>声音与震屏</h2>
 * <ul>
 *   <li>回合开始：<b>完全静音、不震屏</b> —— 每回合都会发生</li>
 *   <li>施放：轻震屏（LOW/SHORT）+ 音效 —— 一局只几次</li>
 * </ul>
 * 音效一律 200ms 去重。
 */
final class FastingFx {
    /** 力量：红。与原版光圈同一色系，但两种属性可分。 */
    static final Color STRENGTH = FastingDraw.STRENGTH;
    /** 敏捷：绿。 */
    static final Color DEXTERITY = FastingDraw.DEXTERITY;
    /** 白热。 */
    static final Color CORE = FastingDraw.CORE;
    /** 被抽空的能量。 */
    static final Color DRAINED = FastingDraw.DRAINED;

    // ---------------- 各角色的能量球配色（光晕用） ----------------
    /** 观者：紫。 */
    private static final Color ENERGY_PURPLE = new Color(0.78f, 0.52f, 1.00f, 1.0f);
    /** 铁甲战士：红。 */
    private static final Color ENERGY_RED = new Color(1.00f, 0.52f, 0.30f, 1.0f);
    /** 静默猎手：绿。 */
    private static final Color ENERGY_GREEN = new Color(0.56f, 1.00f, 0.52f, 1.0f);
    /** 故障机器人：蓝。 */
    private static final Color ENERGY_BLUE = new Color(0.48f, 0.86f, 1.00f, 1.0f);
    /** 通用兜底：暖黄。 */
    private static final Color ENERGY_GOLD = new Color(1.00f, 0.86f, 0.42f, 1.0f);

    /** 胸口相对身高的比例：命中点。 */
    static final float CHEST_RATIO = 0.56f;

    /**
     * 红圈与绿圈之间的错开时间（秒）。
     * <p>
     * 原版光圈自己会活 1.0s，所以 0.14s 的错开足以看出「先后」，
     * 又不会让两圈完全分开。
     */
    static final float RING_STEP = 0.14f;

    private static final String SFX_SURGE = "POWER_STRENGTH";
    private static final HashMap<String, Float> LAST_AT = new HashMap<>();
    private static final float DEDUP = 0.20f;

    /** 缓存算出来的能量色，避免每帧 new。 */
    private static final Color ENERGY_CACHE = new Color();

    private FastingFx() {
    }

    // ---------------- 几何 ----------------

    /** 命中点：观者胸口。 */
    static float chestX(AbstractCreature owner) {
        return BodyFx.centerX(owner);
    }

    static float chestY(AbstractCreature owner) {
        return BodyFx.groundY(owner) + BodyFx.height(owner) * CHEST_RATIO;
    }

    /**
     * 能量球的真实屏幕坐标。
     * <p>
     * 取自 {@code AbstractDungeon.overlayMenu.energyPanel.current_x/current_y} ——
     * 这是原版自己在 {@code EnergyPanel.render()} 里画能量球用的坐标，
     * 所以球一定是从玩家看到的那个能量球里「飞出来」的。
     *
     * @param out 输出 [x, y]
     */
    static void energyOrbPos(float[] out) {
        out[0] = Settings.WIDTH * 0.16f;
        out[1] = Settings.HEIGHT * 0.28f;
        OverlayMenu menu = AbstractDungeon.overlayMenu;
        if (menu == null) {
            return;
        }
        EnergyPanel panel = menu.energyPanel;
        if (panel == null) {
            return;
        }
        out[0] = panel.current_x;
        out[1] = panel.current_y;
    }

    /**
     * 能量球的光晕颜色：跟随当前角色的能量配色。
     * <p>
     * 球体本身用 {@code getEnergyImage()} 贴图（颜色天然正确），
     * 这里只给光晕与拖尾一个同色系的近似色。
     * 颜色对象是共享的，<b>调用方不要修改它</b>。
     */
    static Color energyColor() {
        AbstractPlayer p = AbstractDungeon.player;
        if (p == null || p.chosenClass == null) {
            ENERGY_CACHE.set(ENERGY_GOLD);
            return ENERGY_CACHE;
        }
        switch (p.chosenClass) {
            case WATCHER:
                ENERGY_CACHE.set(ENERGY_PURPLE);
                break;
            case IRONCLAD:
                ENERGY_CACHE.set(ENERGY_RED);
                break;
            case THE_SILENT:
                ENERGY_CACHE.set(ENERGY_GREEN);
                break;
            case DEFECT:
                ENERGY_CACHE.set(ENERGY_BLUE);
                break;
            default:
                // 自定义角色：优先用它的卡牌渲染色，实在没有就用暖金
                try {
                    Color c = p.getCardRenderColor();
                    if (c != null) {
                        ENERGY_CACHE.set(c);
                        ENERGY_CACHE.lerp(Color.WHITE, 0.30f);
                        break;
                    }
                } catch (Throwable ignored) {
                    // 忽略：走兜底
                }
                ENERGY_CACHE.set(ENERGY_GOLD);
                break;
        }
        ENERGY_CACHE.a = 1.0f;
        return ENERGY_CACHE;
    }

    /** 有效层数 = 每回合减几点能量。 */
    static int severity(AbstractCreature owner) {
        if (owner == null || owner.powers == null) {
            return 1;
        }
        AbstractPower p = owner.getPower(PurpleEffectRegistry.POWER_FASTING);
        int n = p == null ? 1 : Math.max(1, Math.abs(p.amount));
        return MathUtils.clamp(n, 1, 3);
    }

    // ---------------- 声音 ----------------

    /** 带去重的单发音效，避免多张斋戒同时施加时炸成一片。 */
    private static void playOnce(String key, float pitchVariation) {
        if (CardCrawlGame.sound == null || key == null) {
            return;
        }
        float now = (float) (System.nanoTime() / 1_000_000_000.0);
        Float last = LAST_AT.get(key);
        if (last != null && now - last < DEDUP) {
            return;
        }
        LAST_AT.put(key, now);
        CardCrawlGame.sound.play(key, pitchVariation);
    }

    // ---------------- 事件入口 ----------------

    /**
     * 回合开始：{@code EnergyDownPower} 扣掉能量。
     * <p>
     * 每回合都会发生，所以演出必须明确、但<b>不能喧闹</b>：完全静音、不震屏。
     * 走 {@code queueTop}：球从屏幕左下角的能量槽飞到观者，
     * 整段轨迹横跨手牌区。
     */
    static void onTurnStart(AbstractCreature owner) {
        if (owner == null) {
            return;
        }
        PurpleEffectRegistry.queueTop(new FastingTollEffect(owner, severity(owner)));
    }

    /**
     * 施放这张牌：一次性的 +力量 / +敏捷。
     * <p>
     * 整张牌<b>唯一</b>允许出声的时刻（一局只几次），所以给轻震屏 + 音效。
     */
    static void onCast(AbstractPlayer player, AbstractCard card) {
        if (player == null) {
            return;
        }
        int amp = card == null ? 3 : Math.max(1, card.magicNumber);
        PurpleEffectRegistry.queue(new FastingSurgeEffect(player, amp));
        playOnce(SFX_SURGE, 0.10f);
        if (CardCrawlGame.screenShake != null) {
            CardCrawlGame.screenShake.shake(
                    ScreenShake.ShakeIntensity.LOW, ScreenShake.ShakeDur.SHORT, false);
        }
    }
}
