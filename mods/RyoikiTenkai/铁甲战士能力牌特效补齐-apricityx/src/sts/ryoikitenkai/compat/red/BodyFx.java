package sts.ryoikitenkai.compat.red;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 角色身体的几何工具。
 * <p>
 * <b>约束：不绑定骨骼。</b> 早先这里有一个从骨架里随机挑骨头的方法，
 * 但各角色、各换肤 mod 的骨骼命名与数量都不一样，绑骨骼既容易失效，
 * 也会让特效随动画抖动。现在全部改成「按 hitbox 的比例采样」：
 * hitbox 在任何角色上都有，且不受动画形变影响，位置稳定可预测。
 * <p>
 * 「落在身上」用 {@link #bodyY} 按身高比例取点，而不是查骨头坐标。
 */
final class BodyFx {
    /** 没有 hitbox 时的兜底身体高度（已乘 scale）。 */
    private static final float FALLBACK_HEIGHT = 200.0f * Settings.scale;
    /** 没有 hitbox 时的兜底身体半宽（已乘 scale）。 */
    private static final float FALLBACK_HALF_WIDTH = 60.0f * Settings.scale;

    private BodyFx() {
    }

    /** 角色脚下中轴 X。 */
    static float centerX(AbstractCreature creature) {
        return creature != null ? creature.hb.cX : Settings.WIDTH * 0.25f;
    }

    /** 角色脚底线 Y。hitbox 底边就是脚底，用它才能把地面效果永远压在脚下。 */
    static float groundY(AbstractCreature creature) {
        return creature != null ? creature.hb.y : Settings.HEIGHT * 0.25f;
    }

    /** 身体高度，带兜底值避免小分辨率下特效贴地。 */
    static float height(AbstractCreature creature) {
        if (creature == null) {
            return FALLBACK_HEIGHT;
        }
        return Math.max(120.0f * Settings.scale, creature.hb.height);
    }

    /** 身体半宽。 */
    static float halfWidth(AbstractCreature creature) {
        if (creature == null) {
            return FALLBACK_HALF_WIDTH;
        }
        return Math.max(45.0f * Settings.scale, creature.hb.width * 0.5f);
    }

    /** 身体范围内的随机 X；spread 为 1 时正好到 hitbox 边缘。 */
    static float bodyX(AbstractCreature creature, float spread) {
        float hw = halfWidth(creature) * MathUtils.clamp(spread, 0.0f, 1.0f);
        return centerX(creature) + MathUtils.random(-hw, hw);
    }

    /**
     * 身体范围内的随机 Y（按身高的比例采样，0 在脚下、1 在头顶）。
     * 这是「落在身上」的实现方式：不查骨骼，只按 hitbox 高度取比例。
     */
    static float bodyY(AbstractCreature creature, float minFraction, float maxFraction) {
        float low = MathUtils.clamp(minFraction, 0.0f, 1.0f);
        float high = Math.max(low, MathUtils.clamp(maxFraction, 0.0f, 1.0f));
        return groundY(creature) + height(creature) * MathUtils.random(low, high);
    }
}
