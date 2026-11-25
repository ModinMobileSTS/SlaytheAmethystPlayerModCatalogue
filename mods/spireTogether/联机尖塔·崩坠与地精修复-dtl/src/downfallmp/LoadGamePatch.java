package downfallmp;

import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;

// 联机读档不经过大厅的开局方法，也要在重建地图前应用房间设置。
@SpirePatch2(cls = "spireTogether.util.LoadGameManager", method = "ImitateGameLoading",
    paramtypez = {}, requiredModId = "downfall", optional = true)
public final class LoadGamePatch {
    private LoadGamePatch() {
    }

    @SpirePrefixPatch
    public static void Prefix() {
        DownfallMPMode.applyRoomSettings();
    }
}
