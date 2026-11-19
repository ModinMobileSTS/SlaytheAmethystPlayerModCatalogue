package mpdownfallmap;

import downfall.monsters.FleeingMerchant;

/**
 * 联机局里“逃跑商人”的残血继承记录。
 *
 * 崩坠的设计是：商人血很厚，会偷钱然后跑；打掉的血会留下来，
 * 下次遇到他还是残血，这是给玩家的反制机会，必须保留。
 *
 * 问题在于 Downfall 把这个残血值放在静态字段 FleeingMerchant.CURRENT_HP 里，
 * 这个字段是“绝对血量”，而且每个客户端各存一份；联机模组又会按人数把血量上限
 * 从 400 放大到 800，还在自己的补丁里对这个字段做乘除缩放，
 * 于是两台机器的数字完全对不上：400/800、263/800、甚至被缩到 0，一进场就死。
 *
 * 做法：联机局里改用“剩余血量比例”来记残血（0~1，与人数、血量上限都无关），
 * 每次商人出场时按当前的上限换算回绝对血量，两台机器算出来完全一致；
 * 比例在商人每个回合结束后按当时的 当前血量 / 上限 更新，因此该继承的残血依然继承。
 * 单机完全不介入。
 */
final class MerchantCarryOver {
    /** 上一次交手结束时商人剩余的血量比例；新开一局是 1.0（满血）。 */
    private static float fraction = 1.0f;

    private MerchantCarryOver() {
    }

    /** 新的一局开始：商人满血。 */
    static void reset() {
        fraction = 1.0f;
    }

    /** 商人出场：把残血比例换算成这个客户端当前上限下的绝对血量。 */
    static void applyTo(FleeingMerchant merchant) {
        if (merchant == null || merchant.maxHealth <= 0) {
            return;
        }
        int hp = Math.round(merchant.maxHealth * fraction);
        if (hp < 1) {
            hp = 1;
        }
        if (hp > merchant.maxHealth) {
            hp = merchant.maxHealth;
        }
        merchant.currentHealth = hp;
        FleeingMerchant.CURRENT_HP = hp;
    }

    /** 商人每个回合结束后记录残血比例，跑掉/被打死之前的最后一次记录就是继承值。 */
    static void record(FleeingMerchant merchant) {
        if (merchant == null || merchant.maxHealth <= 0) {
            return;
        }
        float value = merchant.currentHealth / (float) merchant.maxHealth;
        if (value < 0.0f) {
            value = 0.0f;
        }
        if (value > 1.0f) {
            value = 1.0f;
        }
        fraction = value;
    }
}