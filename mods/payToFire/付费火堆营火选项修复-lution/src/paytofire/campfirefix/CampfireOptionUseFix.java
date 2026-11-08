package paytofire.campfirefix;

import PayToFire.modcore.payToFire;
import PayToFire.patchs.CamfirePatch;
import PayToFire.patchs.PayOption;
import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.rooms.CampfireUI;
import com.megacrit.cardcrawl.rooms.RestRoom;
import java.lang.reflect.Field;
import java.util.ArrayList;

/**
 * 付费火堆（PayToFire）：营火选项生效修复 + 递增计费修复。
 *
 * 一、选项点了没反应（v0.1.0 修复）
 * BaseMod 的 PostCampfireHook 把 AbstractCampfireOption.update() 里
 * “campfireUI.somethingSelected = true” 改写成
 * “campfireUI.somethingSelected = BaseMod.publishPostCampfire()”，
 * 也就是每次点击营火选项都会立刻回调 PostCampfireSubscriber。
 * 而启动器的触摸二次确认兼容层（AmethystRuntimeCompat 的
 * CampfireDesktopTouchConfirmPatches）会把 useOption() 推迟到再点一次确认按钮，
 * 此时它先把当前选项记为 suppressedOption 并返回 false。
 * 于是模组的 receivePostCampfire() 提前把其它选项全部禁用、熄灭火焰，
 * 兼容层随后发现选项已不可用就取消了待确认状态 —— useOption() 永远不会执行。
 * 处理：在“已被推迟、等待确认”的这次回调里跳过模组的营火后处理。
 *
 * 二、递增计费不对（v0.2.0 修复）
 * 模组的计费是 campfirecost + dizeng * cishu（CamfirePatch.cishu），
 * 但 cishu 是在 receivePostCampfire() 里自增的，而这个回调发生在
 * “点击点火”的瞬间、真正扣钱（PayEffect，0.5 秒后）之前，
 * 并且使用休息/锻造等选项时还会再自增一次。结果：
 *   - 扣钱时已经多算了一次递增：第一次点火显示 15 却扣 15+35=50；
 *   - 每轮“点火 + 使用”让次数涨 2，价格按两倍速度暴涨；
 *   - 界面上显示的价格和 haveEnoughMoney() 的金币检查都比实际扣费慢一步。
 * 处理：把“次数”改成只在一次点火真正付款完成时 +1，
 * 并在 +1 之后立刻刷新点火选项的显示价格与可用状态，
 * 使“显示价格 = 金币检查 = 实际扣费”始终保持一致。
 */
@SpirePatch2(
    cls = "PayToFire.modcore.payToFire",
    method = "receivePostCampfire",
    paramtypez = {}
)
public final class CampfireOptionUseFix {

    /** 触摸确认兼容层的补丁类；不存在时相关逻辑自动退化为无操作。 */
    private static final String TOUCH_CONFIRM_PATCH_CLASS =
            "io.stamethyst.compatmod.touch.CampfireDesktopTouchConfirmPatches";

    private static boolean touchConfirmResolved = false;
    private static Field deferredOptionField = null;

    /** 本次点击营火选项之前的“点火次数”，也就是本次付款应当使用的递增档位。 */
    private static int cishuBeforeClick = -1;

    private CampfireOptionUseFix() {
    }

    private static void resolveTouchConfirmAccess() {
        if (touchConfirmResolved) {
            return;
        }
        touchConfirmResolved = true;
        try {
            Class<?> touchConfirmClass = Class.forName(TOUCH_CONFIRM_PATCH_CLASS);
            Field field = touchConfirmClass.getDeclaredField("suppressedOption");
            field.setAccessible(true);
            deferredOptionField = field;
        } catch (Throwable ignored) {
            deferredOptionField = null;
        }
    }

    /**
     * 兼容层是否刚刚把某个营火选项的 useOption() 推迟到确认按钮上执行。
     * 若该兼容层不存在，总是返回 false（保持模组原本的行为）。
     */
    public static boolean isOptionUseDeferred() {
        resolveTouchConfirmAccess();
        if (deferredOptionField == null) {
            return false;
        }
        try {
            return deferredOptionField.get(null) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SpirePrefixPatch
    public static SpireReturn<Boolean> Prefix() {
        cishuBeforeClick = CamfirePatch.cishu;
        if (isOptionUseDeferred()) {
            // 选项还没有真正执行，此时不能禁用选项/熄灭火焰，否则确认会被取消。
            return SpireReturn.Return(Boolean.TRUE);
        }
        return SpireReturn.Continue();
    }

    @SpirePostfixPatch
    public static void Postfix() {
        // 撤销模组在这次点击里做的次数自增：扣费、金币检查与显示价格都使用点击前的档位，
        // 真正的 +1 挪到付款完成时（见 AdvancePaidCount）。
        if (cishuBeforeClick >= 0 && CamfirePatch.cishu > cishuBeforeClick) {
            CamfirePatch.cishu = cishuBeforeClick;
        }
    }

    /**
     * 一次点火付款完成（PayEffect 已扣钱）后，才把火堆次数 +1，
     * 并刷新点火选项的显示价格与可用状态。
     */
    @SpirePatch2(
        cls = "PayToFire.patchs.PayEffect",
        method = "EnableAllbuttons",
        paramtypez = {}
    )
    public static class AdvancePaidCount {
        @SpirePostfixPatch
        public static void Postfix() {
            if (cishuBeforeClick < 0) {
                return;
            }
            int paidCount = cishuBeforeClick + 1;
            if (CamfirePatch.cishu < paidCount) {
                CamfirePatch.cishu = paidCount;
            }
            refreshPayOption();
        }
    }

    private static void refreshPayOption() {
        try {
            if (!(AbstractDungeon.getCurrRoom() instanceof RestRoom)) {
                return;
            }
            CampfireUI ui = ((RestRoom) AbstractDungeon.getCurrRoom()).campfireUI;
            if (ui == null) {
                return;
            }
            ArrayList buttons = (ArrayList) ReflectionHacks.getPrivate(ui, CampfireUI.class, "buttons");
            if (buttons == null) {
                return;
            }
            boolean affordable = AbstractDungeon.player.gold >= CamfirePatch.getTrueCost();
            for (Object item : buttons) {
                if (!(item instanceof PayOption)) {
                    continue;
                }
                PayOption pay = (PayOption) item;
                if (pay.usable && !affordable) {
                    // 原逻辑是按“旧价格”判断并通过的，按真实价格应当付不起：
                    // 禁用点火选项，并把这次被消耗的额外使用次数还回去。
                    pay.usable = false;
                    ++payToFire.Trueduoci;
                }
                pay.updateUsability(pay.usable);
            }
        } catch (Throwable ignored) {
            // 仅用于刷新界面文本，任何异常都不应影响游戏流程
        }
    }
}
