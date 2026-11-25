package downfallmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

public final class DungeonStartPatches {
    private DungeonStartPatches() {}

    // 中文大厅和原版联机大厅都在创建角色及地图前调用这个方法。
    @SpirePatch2(clz = AbstractDungeon.class, method = "generateSeeds", paramtypez = {}, requiredModId = "downfall", optional = true)
    public static class BeforeSeeds {
        public static void Prefix() {
            DownfallMPMode.applyRoomSettings();
        }
    }

    // 真正退回主菜单时再清理，避免联机先断开、结算画面还没结束就关掉崩坠。
    @SpirePatch2(clz = AbstractDungeon.class, method = "reset", paramtypez = {}, requiredModId = "downfall", optional = true)
    public static class AfterReset {
        public static void Postfix() {
            DownfallMPMode.resetAfterRun();
        }
    }
}
