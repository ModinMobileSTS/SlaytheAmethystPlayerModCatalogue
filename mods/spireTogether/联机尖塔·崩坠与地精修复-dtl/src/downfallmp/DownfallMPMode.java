package downfallmp;

import spireTogether.SpireTogetherMod;
import spireTogether.modcompat.downfall.patches.DownfallMapSelectionPatches;
import spireTogether.network.P2P.P2PClientData;
import spireTogether.network.P2P.P2PManager;
import spireTogether.network.objects.settings.GameSettings;
import spireTogether.network.objects.settings.NetworkCustomMod;

final class DownfallMPMode {
    static final String EVIL_RUN_ID = "downfall:EvilRun";
    private static boolean appliedToRun;
    private static boolean soulDisplay;

    private DownfallMPMode() {}

    static boolean requested(GameSettings settings) {
        if (settings == null) return false;
        if (settings.extraData != null && settings.getBooleanSetting(DownfallMapSelectionPatches.downfallMapSetting)) {
            return true;
        }
        if (settings.customMods != null) {
            for (NetworkCustomMod mod : settings.customMods) {
                if (mod != null && EVIL_RUN_ID.equals(mod.id)) return true;
            }
        }
        return false;
    }

    static void applyRoomSettings() {
        if (!SpireTogetherMod.isConnected) return;
        P2PClientData data = P2PManager.data;
        if (data == null || data.settings == null) return;

        boolean enabled = requested(data.settings);
        if (!DownfallBridge.setMode(enabled)) return;
        appliedToRun = true;
        // 崩坠还会读取单人自定义屏幕和每日挑战的标记。联机只认本房间的设置。
        DownfallBridge.clearSoloSelections();
        if (enabled != soulDisplay) {
            DownfallBridge.syncGoldDisplay(enabled);
            soulDisplay = enabled;
        }
    }

    static void resetAfterRun() {
        if (!appliedToRun) return;
        appliedToRun = false;
        DownfallBridge.setMode(false);
        DownfallBridge.clearSoloSelections();
        if (soulDisplay) {
            DownfallBridge.syncGoldDisplay(false);
            soulDisplay = false;
        }
    }
}
