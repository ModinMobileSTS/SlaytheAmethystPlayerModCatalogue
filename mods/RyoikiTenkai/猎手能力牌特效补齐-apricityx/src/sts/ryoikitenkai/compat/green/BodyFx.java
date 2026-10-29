package sts.ryoikitenkai.compat.green;

import com.badlogic.gdx.math.MathUtils;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 角色身体的几何工具。
 * <p>
 * <b>约束：不绑定骨骼</b>（规范 §1.1）。坐标只从 {@code owner.hb}（判定框）与地面推导：
 * <ul>
 *   <li>{@code hb.y} 就是脚底线（{@code refreshHitboxLocation} 里
 *       {@code hb.cY = drawY + hb_y + hb_h / 2}）</li>
 *   <li>身高、半宽都用 {@code hb} 的比例，不写死像素，否则窄体型角色上特效会挤成一团</li>
 * </ul>
 * 早先这里还有一组走 {@code ReflectionHacks} 读 spine {@code skeleton}、
 * 再从里面挑骨头的工具（{@code skeleton()} / {@code randomBodyBone()}）。它们有两个问题：
 * <ol>
 *   <li>各角色、各换肤 mod 的骨头命名与数量都不一样，按世界坐标筛也会挑到会甩动的手臂骨，
 *       特效跟着待机动画抖</li>
 *   <li><b>已经没有任何调用点了</b> —— 本补丁所有特效都改成按 {@code hb} 比例采样，
 *       留着只会让人以为还在用（《指引》§9：改版后要删掉被取代的代码）</li>
 * </ol>
 * 所以整组骨骼工具已删除，本类只剩纯几何。
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

    /** 身体高度，带兜底值，避免小分辨率下特效贴地。 */
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

    /** 胸腔高度：环、卡影、刀影都以它为纵向基准。 */
    static float chestY(AbstractCreature creature) {
        return groundY(creature) + height(creature) * 0.56f;
    }

    /** 身体范围内的随机 X；spread 为 1 时正好到 hitbox 边缘。 */
    static float bodyX(AbstractCreature creature, float spread) {
        float hw = halfWidth(creature) * MathUtils.clamp(spread, 0.0f, 1.0f);
        return centerX(creature) + MathUtils.random(-hw, hw);
    }

    /** 身体范围内的随机 Y（按身高的比例，0 在脚下，1 在头顶）。 */
    static float bodyY(AbstractCreature creature, float minFraction, float maxFraction) {
        float low = MathUtils.clamp(minFraction, 0.0f, 1.0f);
        float high = Math.max(low, MathUtils.clamp(maxFraction, 0.0f, 1.0f));
        return groundY(creature) + height(creature) * MathUtils.random(low, high);
    }
}
