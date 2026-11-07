package skindexunlock;

import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import skindex.SkindexDev;

/**
 * Skindex 皮肤解锁补丁入口。
 *
 * <p>Skindex 用 {@code OwnableItem.canUse()} 作为“该皮肤是否已拥有/可用”的唯一判定，
 * 并且在该方法开头提供了一个官方总开关 {@code SkindexDev.unlockAll}。
 * 这里在模组加载时把它打开，使所有皮肤在角色与皮肤自定义界面、
 * Neow 衣柜轮换列表以及 {@code SkindexRegistry} 的 onlyOwned 过滤中都被视为已解锁。</p>
 */
@SpireInitializer
public final class SkinUnlockMod {

    public SkinUnlockMod() {
        SkindexDev.unlockAll = true;
    }

    public static void initialize() {
        new SkinUnlockMod();
    }
}
