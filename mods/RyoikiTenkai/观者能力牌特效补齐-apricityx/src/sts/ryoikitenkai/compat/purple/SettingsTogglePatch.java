package sts.ryoikitenkai.compat.purple;

import basemod.ReflectionHacks;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import sts.ryoikitenkai.modcore.RyoikiTenkai;

import java.util.ArrayList;

/**
 * 把本补丁涉及的卡牌补进父模组的设置面板开关列表。
 * <p>
 * 父模组的「禁用特效」面板是遍历 {@code RyoikiTenkai.doneCards} 建按钮的，
 * 而那张表只列了父模组自己实现的 37 张牌。本补丁追加的观者牌都不在里面，
 * 于是出现「特效开着就再也关不掉」的缺口。
 * <p>
 * {@code doneCards} 是包私有字段，本补丁不在 {@code sts.ryoikitenkai.modcore} 包里，
 * 因此走 {@link ReflectionHacks} 取引用；拿到的是同一个 ArrayList 实例，成员可写。
 * 这里在父模组组装面板<b>之前</b>（{@code receivePostInitialize} 的 Prefix）补登记，
 * 已存在则不重复添加 —— 父模组以后自己加进来也不会出现两个同名开关。
 * 必须是 Prefix 而不是 Postfix：Postfix 时 {@code SettingsPanel} 已经建完了。
 * <p>
 * 全程吞异常：这是纯附属的界面修补，任何反射失败都不该影响战斗内特效。
 * <p>
 * <b>本版变更</b>：「以手拒之」（{@code TalkToTheHand}）的特效已整体移出本补丁，
 * 所以不再给它建开关 —— 面板里只列<b>真正还会放特效</b>的 12 张能力牌。
 */
@SpirePatch2(
    cls = "sts.ryoikitenkai.modcore.RyoikiTenkai",
    method = "receivePostInitialize",
    paramtypez = {}
)
public final class SettingsTogglePatch {
    private SettingsTogglePatch() {
    }

    /**
     * 本补丁实际会放特效、但父模组面板里没有开关的卡牌 ID。
     * 只列真正实现了特效的牌，避免面板里出现无效开关。
     */
    private static final String[] EXTRA_CARDS = {
            PurpleEffectRegistry.CARD_BATTLE_HYMN,
            PurpleEffectRegistry.CARD_ESTABLISHMENT,
            PurpleEffectRegistry.CARD_FASTING,
            PurpleEffectRegistry.CARD_LIKE_WATER,
            PurpleEffectRegistry.CARD_MASTER_REALITY,
            PurpleEffectRegistry.CARD_MENTAL_FORTRESS,
            PurpleEffectRegistry.CARD_NIRVANA,
            PurpleEffectRegistry.CARD_OMEGA,
            PurpleEffectRegistry.CARD_RUSHDOWN,
            PurpleEffectRegistry.CARD_STUDY,
            PurpleEffectRegistry.CARD_FORESIGHT
    };

    @SpirePrefixPatch
    public static void Prefix() {
        try {
            Object raw = ReflectionHacks.getPrivateStatic(RyoikiTenkai.class, "doneCards");
            if (!(raw instanceof ArrayList)) {
                return;
            }
            @SuppressWarnings("unchecked")
            ArrayList<String> cards = (ArrayList<String>) raw;
            for (String id : EXTRA_CARDS) {
                if (id != null && !cards.contains(id)) {
                    cards.add(id);
                }
            }
        } catch (Throwable ignored) {
            // 面板补登记失败不影响任何战斗内特效，静默跳过。
        }
    }
}
