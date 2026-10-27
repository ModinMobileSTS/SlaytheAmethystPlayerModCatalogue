package sts.ryoikitenkai.compat.green;

import basemod.ReflectionHacks;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.esotericsoftware.spine.Bone;
import com.esotericsoftware.spine.Skeleton;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.Settings;

/**
 * 角色身体的几何与骨骼查询工具。
 * <p>
 * 本补丁的特效都贴在角色身上，位置不能写死：角色会移动、会摆动、会切换动作。
 * 统一从 {@link AbstractCreature} 的 hitbox 取基准，并从 protected 的 skeleton 字段取骨骼，
 * 让粒子落在「身体上」而不是空中。
 * <p>
 * skeleton 是 protected 字段，只能通过 ReflectionHacks 读取（父模组也是这么取的）。
 */
final class BodyFx {
    private static final float FALLBACK_HEIGHT = 200.0f * Settings.scale;
    private static final float FALLBACK_HALF_WIDTH = 60.0f * Settings.scale;

    private BodyFx() {
    }

    static Skeleton skeleton(AbstractCreature creature) {
        if (creature == null) {
            return null;
        }
        try {
            return ReflectionHacks.getPrivate(creature, AbstractCreature.class, "skeleton");
        } catch (Throwable t) {
            return null;
        }
    }

    /** 角色脚下中轴 X。 */
    static float centerX(AbstractCreature creature) {
        return creature != null ? creature.hb.cX : Settings.WIDTH * 0.25f;
    }

    /** 角色脚底线 Y。hitbox 底边就是脚底。 */
    static float groundY(AbstractCreature creature) {
        return creature != null ? creature.hb.y : Settings.HEIGHT * 0.25f;
    }

    /** 身体高度，带兜底值避免缩小分辨率下粒子贴地。 */
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

    /**
     * 从骨架里随机挑一根「落在身体范围内」的骨头。
     * <p>
     * 不按名字挑：原版各角色、各种换肤 mod 的骨头命名都不一样，写死名字会失效；
     * 按骨骼当前世界坐标是否落在 hitbox 内来筛，配合动画形变依旧成立。
     */
    static Bone randomBodyBone(Skeleton skeleton, AbstractCreature creature) {
        if (skeleton == null || creature == null) {
            return null;
        }
        Array<Bone> bones = skeleton.getBones();
        if (bones == null || bones.size == 0) {
            return null;
        }
        float minY = creature.hb.y + creature.hb.height * 0.12f;
        float maxY = creature.hb.y + creature.hb.height * 0.95f;
        float maxDx = Math.max(creature.hb.width * 0.75f, 40.0f * Settings.scale);
        int start = MathUtils.random(bones.size - 1);
        for (int i = 0; i < bones.size; ++i) {
            Bone bone = bones.get((start + i) % bones.size);
            if (bone == null) {
                continue;
            }
            float wx = bone.getWorldX() + skeleton.getX();
            float wy = bone.getWorldY() + skeleton.getY();
            if (Math.abs(wx - creature.hb.cX) <= maxDx && wy >= minY && wy <= maxY) {
                return bone;
            }
        }
        return null;
    }
}
