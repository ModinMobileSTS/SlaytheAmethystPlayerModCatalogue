package sts.ryoikitenkai.compat.purple;

import com.badlogic.gdx.graphics.Color;
import com.megacrit.cardcrawl.core.AbstractCreature;

import java.util.WeakHashMap;

/**
 * 心之堡垒的「姿态快照」：记住切换前后的姿态，供爆发特效取色。
 * <p>
 * 为什么需要它：{@code MentalFortressPower.onChangeStance} 只能拿到新旧两个
 * {@code AbstractStance}，而 {@link PurpleEffectRegistry#burst} 是统一入口、不认识姿态。
 * 这里用一张按生物索引的弱表把最近一次切换暂存下来，爆发时再取。
 * 表是 {@link WeakHashMap}，生物被回收时自动清理，不需要手动释放。
 * <p>
 * 姿态 → 颜色与 {@link FortressAuraEffect} 保持一致，玩家在爆发里看到的颜色
 * 和常驻护壁是同一套，读得出「从平静切到了愤怒」。
 */
final class FortressPulse {
    private static final Color CALM = new Color(0.46f, 0.90f, 1.00f, 1.0f);
    private static final Color WRATH = new Color(1.00f, 0.55f, 0.30f, 1.0f);
    private static final Color DIVINITY = new Color(1.00f, 0.88f, 0.45f, 1.0f);
    private static final Color NEUTRAL = new Color(0.72f, 0.80f, 1.00f, 1.0f);

    /** 每个生物最近一次姿态切换的三元信息。 */
    private static final WeakHashMap<AbstractCreature, Snapshot> LAST = new WeakHashMap<>();

    private static final class Snapshot {
        final Color oldColor;
        final Color newColor;
        final String newStance;

        Snapshot(Color oldColor, Color newColor, String newStance) {
            this.oldColor = oldColor;
            this.newColor = newColor;
            this.newStance = newStance;
        }
    }

    private FortressPulse() {
    }

    /** 姿态 → 颜色。未知姿态或 null 走中性色。 */
    static Color colorOf(String stance) {
        if ("Calm".equals(stance)) {
            return CALM;
        }
        if ("Wrath".equals(stance)) {
            return WRATH;
        }
        if ("Divinity".equals(stance)) {
            return DIVINITY;
        }
        return NEUTRAL;
    }

    /** 记下这次切换。 */
    static void remember(AbstractCreature owner, String oldStance, String newStance) {
        if (owner == null) {
            return;
        }
        LAST.put(owner, new Snapshot(colorOf(oldStance), colorOf(newStance), newStance));
    }

    static Color lastOld(AbstractCreature owner) {
        Snapshot s = owner == null ? null : LAST.get(owner);
        return s == null ? NEUTRAL : s.oldColor;
    }

    static Color lastNew(AbstractCreature owner) {
        Snapshot s = owner == null ? null : LAST.get(owner);
        return s == null ? NEUTRAL : s.newColor;
    }

    static String lastNewStance(AbstractCreature owner) {
        Snapshot s = owner == null ? null : LAST.get(owner);
        return s == null ? null : s.newStance;
    }
}
