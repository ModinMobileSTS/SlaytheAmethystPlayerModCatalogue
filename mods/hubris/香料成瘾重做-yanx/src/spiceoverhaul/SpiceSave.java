package spiceoverhaul;

import basemod.BaseMod;
import basemod.abstracts.CustomSavableRaw;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/** Persists the "already bottomed out once" flag inside the run save file. */
@SpireInitializer
public class SpiceSave implements CustomSavableRaw {
    public static final String KEY = "spiceReworkBottomed";

    private static final SpiceSave INSTANCE = new SpiceSave();

    public static void initialize() {
        BaseMod.addSaveField(KEY, INSTANCE);
    }

    @Override
    public JsonElement onSaveRaw() {
        return new JsonPrimitive(SpiceData.bottomedOnce);
    }

    @Override
    public void onLoadRaw(JsonElement json) {
        boolean value = false;
        try {
            if (json != null && json.isJsonPrimitive()) {
                value = json.getAsBoolean();
            }
        } catch (Throwable ignored) {
        }
        SpiceData.bottomedOnce = value;
    }
}
