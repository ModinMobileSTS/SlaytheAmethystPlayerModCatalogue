package mpdownfallmap;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;

import downfall.downfallMod;
import downfall.monsters.FleeingMerchant;

/**
 * 让联机局里的逃跑商人血量在两个客户端上保持一致，同时保留崩坠“残血继承”的玩法。
 *
 * 商人出场时按“残血比例 × 当前血量上限”算出该有多少血（见 MerchantCarryOver），
 * 每个回合结束后更新比例，新开一局时比例回到满血。
 * 这样：
 *   - 新一局第一次遇到 = 满血；
 *   - 打掉一半让他跑了，下次遇到 = 半血（原本的反制机会还在）；
 *   - 两台机器算出来的数字一致，不会再出现 400/800、263/800 或 0 血开打。
 *
 * 单机不介入，Downfall 原本的绝对血量继承逻辑照常工作。
 */
public final class FleeingMerchantHpSync {
    private FleeingMerchantHpSync() {
    }

    /** 商人被创建（出场）时把血量定下来。 */
    @SpirePatch2(clz = FleeingMerchant.class, method = "<ctor>", paramtypez = {})
    public static class OnSpawn {
        @SpirePostfixPatch
        public static void Postfix(FleeingMerchant __instance) {
            if (!MpSupport.isConnected()) {
                return;
            }
            MerchantCarryOver.applyTo(__instance);
        }
    }

    /** 商人每个回合结束后记录剩余血量比例。 */
    @SpirePatch2(clz = FleeingMerchant.class, method = "takeTurn", paramtypez = {})
    public static class OnTakeTurn {
        @SpirePostfixPatch
        public static void Postfix(FleeingMerchant __instance) {
            if (!MpSupport.isConnected()) {
                return;
            }
            MerchantCarryOver.record(__instance);
        }
    }

    /** 新的一局开始，残血比例清零（商人满血）。 */
    @SpirePatch2(clz = downfallMod.class, method = "receiveStartGame", paramtypez = {})
    public static class OnRunStart {
        @SpirePostfixPatch
        public static void Postfix() {
            if (!MpSupport.isConnected()) {
                return;
            }
            MerchantCarryOver.reset();
        }
    }
}