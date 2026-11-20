package patches;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import java.lang.reflect.Field;
import java.util.Map;

final class VanillaCardTranslation {
    private VanillaCardTranslation() {
    }

    static void apply() {
        JsonParser parser = new JsonParser();
        Gson gson = new Gson();
        JsonObject source = parser.parse(Gdx.files.internal("StelliveResources/localization/eng/Cardstrings.json").readString("UTF-8")).getAsJsonObject();
        JsonObject vanilla = parser.parse(Gdx.files.internal("localization/zhs/cards.json").readString("UTF-8")).getAsJsonObject();
        JsonObject names = parser.parse(Gdx.files.internal("stellivezh/localization/vanilla_card_names.json").readString("UTF-8")).getAsJsonObject();
        try {
            Field field = LocalizedStrings.class.getDeclaredField("cards");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, CardStrings> cards = (Map<String, CardStrings>) field.get(null);
            int translated = 0;
            StringBuilder skipped = new StringBuilder();
            for (Map.Entry<String, JsonElement> entry : names.entrySet()) {
                String id = entry.getKey();
                CardStrings current = cards.get(id);
                JsonElement korean = source.get(id);
                JsonElement chinese = vanilla.get(id);
                if (id.indexOf(':') >= 0 || current == null || korean == null || chinese == null) {
                    skipped.append(id).append(" (missing); ");
                    continue;
                }
                CardStrings expected = gson.fromJson(korean, CardStrings.class);
                if (!gson.toJsonTree(current).equals(gson.toJsonTree(expected))) {
                    skipped.append(id).append(" (already changed); ");
                    continue;
                }
                CardStrings replacement = gson.fromJson(chinese, CardStrings.class);
                if (replacement == null || replacement.DESCRIPTION == null) {
                    skipped.append(id).append(" (missing Chinese); ");
                    continue;
                }
                current.NAME = entry.getValue().getAsString();
                current.DESCRIPTION = replacement.DESCRIPTION;
                current.UPGRADE_DESCRIPTION = replacement.UPGRADE_DESCRIPTION;
                current.EXTENDED_DESCRIPTION = replacement.EXTENDED_DESCRIPTION;
                translated++;
            }
            BaseMod.logger.info("Stellive Chinese: localized " + translated + " parent-overridden vanilla cards");
            if (skipped.length() > 0) BaseMod.logger.warn("Stellive Chinese: skipped vanilla cards: " + skipped);
        } catch (ReflectiveOperationException e) {
            BaseMod.logger.warn("Stellive Chinese: could not access vanilla card strings", e);
        }
    }
}
