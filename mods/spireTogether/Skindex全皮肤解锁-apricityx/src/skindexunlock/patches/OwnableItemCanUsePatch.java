package skindexunlock.patches;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import skindex.itemtypes.OwnableItem;
import skindex.skins.player.PlayerSkin;

/**
 * 让所有角色皮肤（{@link PlayerSkin}）的 canUse() 始终返回 true。
 *
 * <p>父模组里 canUse() 的结果决定：</p>
 * <ul>
 *   <li>自定义界面里 “LOCKED” 遮罩与 “SET AS DEFAULT” 按钮是否可用；</li>
 *   <li>{@code SkindexRegistry.getSkinsForClass(class, true)} 的 onlyOwned 过滤，
 *       进而决定 Neow 衣柜左右箭头能轮换出哪些皮肤；</li>
 *   <li>解锁描述是否显示为 “Unlocked!”。</li>
 * </ul>
 *
 * <p>这里只在运行时给出“已解锁”的判定，不写入玩家的解锁存档，
 * 因此禁用本补丁后玩家原有的解锁进度保持不变。</p>
 */
@SpirePatch2(
    cls = "skindex.itemtypes.OwnableItem",
    method = "canUse",
    paramtypez = {}
)
public final class OwnableItemCanUsePatch {

    private OwnableItemCanUsePatch() {
    }

    @SpirePrefixPatch
    public static SpireReturn<Boolean> Prefix(OwnableItem __instance) {
        if (__instance instanceof PlayerSkin) {
            return SpireReturn.Return(Boolean.TRUE);
        }
        return SpireReturn.Continue();
    }
}
