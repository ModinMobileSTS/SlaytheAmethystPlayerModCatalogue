package spiceoverhaul;

import basemod.BaseMod;
import basemod.abstracts.CustomSavableRaw;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Persists the Spice recovery counter inside the run save file. */
@SpireInitializer
public class SpiceSave implements CustomSavableRaw {
    public static final String KEY = "spiceReworkRecovery";

    private static final SpiceSave INSTANCE = new SpiceSave();

    public static void initialize() {
        BaseMod.addSaveField(KEY, INSTANCE);
    }

    @Override
    public JsonElement onSaveRaw() {
        return new JsonPrimitive(SpiceData.recoveryCharge);
    }

    @Override
    public void onLoadRaw(JsonElement json) {
        int value = 0;
        try {
            if (json != null && json.isJsonPrimitive()) {
                value = json.getAsInt();
            }
        } catch (Throwable ignored) {
        }
        SpiceData.recoveryCharge = Math.max(0, Math.min(SpiceData.RECOVERY_NEED, value));
    }
}
