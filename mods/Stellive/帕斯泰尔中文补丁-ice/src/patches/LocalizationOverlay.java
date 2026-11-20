package patches;

import basemod.BaseMod;
import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.megacrit.cardcrawl.localization.LocalizedStrings;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LocalizationOverlay {
    private static final Pattern PLACEHOLDER = Pattern.compile("![A-Za-z0-9_:]+!|\\{[^{}]+\\}|\\[(?:E|W|Stellive:[A-Za-z0-9_]+)\\]");

    static void load(Class<?> type, String table, String originalFile, String translatedFile) {
        JsonParser parser = new JsonParser();
        JsonObject original = parser.parse(Gdx.files.internal("StelliveResources/localization/eng/" + originalFile).readString("UTF-8")).getAsJsonObject();
        JsonObject translated = parser.parse(Gdx.files.internal("stellivezh/localization/" + translatedFile).readString("UTF-8")).getAsJsonObject();
        JsonObject result = new JsonObject();
        StringBuilder failures = new StringBuilder();
        int fields = 0;
        for (Map.Entry<String, JsonElement> entry : translated.entrySet()) {
            String id = entry.getKey();
            JsonElement source = original.get(id);
            if (!id.startsWith("Stellive:") || source == null || !source.isJsonObject() || !entry.getValue().isJsonObject()) {
                failures.append(id).append(" missing source; ");
                continue;
            }
            JsonObject merged = source.getAsJsonObject();
            for (Map.Entry<String, JsonElement> field : entry.getValue().getAsJsonObject().entrySet()) {
                JsonElement oldValue = merged.get(field.getKey());
                JsonElement newValue = field.getValue();
                if (compatible(oldValue, newValue)) {
                    merged.add(field.getKey(), newValue);
                    fields++;
                } else {
                    failures.append(id).append('.').append(field.getKey()).append(" incompatible; ");
                }
            }
            result.add(id, merged);
        }
        if ("cards.json".equals(translatedFile)) {
            for (Map.Entry<String, JsonElement> entry : original.entrySet()) {
                if (!entry.getKey().startsWith("Stellive:") || !entry.getValue().isJsonObject()) continue;
                JsonElement translatedEntry = translated.get(entry.getKey());
                if (translatedEntry == null || !translatedEntry.isJsonObject()) {
                    failures.append(entry.getKey()).append(" untranslated; ");
                    continue;
                }
                for (Map.Entry<String, JsonElement> field : entry.getValue().getAsJsonObject().entrySet()) {
                    if (!translatedEntry.getAsJsonObject().has(field.getKey())) {
                        failures.append(entry.getKey()).append('.').append(field.getKey()).append(" untranslated; ");
                    }
                }
            }
        }
        if (failures.length() != 0) {
            BaseMod.logger.warn("Stellive Chinese: untranslated or incompatible fields in " + translatedFile + ": " + failures);
        }
        BaseMod.loadCustomStrings(type, result.toString());
        verifyLoaded(table, translatedFile, result);
        BaseMod.logger.info("Stellive Chinese: loaded " + translatedFile + " (" + result.entrySet().size() + " entries, " + fields + " fields)");
    }

    private static void verifyLoaded(String table, String translatedFile, JsonObject expected) {
        try {
            Field field = LocalizedStrings.class.getDeclaredField(table);
            field.setAccessible(true);
            Map<?, ?> live = (Map<?, ?>) field.get(null);
            Gson gson = new Gson();
            StringBuilder failures = new StringBuilder();
            for (Map.Entry<String, JsonElement> entry : expected.entrySet()) {
                Object value = live.get(entry.getKey());
                if (value == null || !gson.toJsonTree(value).equals(entry.getValue())) {
                    failures.append(entry.getKey()).append("; ");
                }
            }
            if (failures.length() > 0) {
                BaseMod.logger.warn("Stellive Chinese: live table mismatch in " + translatedFile + ": " + failures);
            } else {
                BaseMod.logger.info("Stellive Chinese: verified live table for " + translatedFile);
            }
        } catch (ReflectiveOperationException e) {
            BaseMod.logger.warn("Stellive Chinese: could not verify live table " + table, e);
        }
    }

    private static boolean compatible(JsonElement oldValue, JsonElement newValue) {
        if (oldValue == null || newValue == null || oldValue.isJsonNull() || newValue.isJsonNull()) return false;
        if (oldValue.isJsonArray() && newValue.isJsonArray()) {
            JsonArray oldArray = oldValue.getAsJsonArray();
            JsonArray newArray = newValue.getAsJsonArray();
            if (oldArray.size() != newArray.size()) return false;
            for (int i = 0; i < oldArray.size(); i++) {
                if (!compatible(oldArray.get(i), newArray.get(i))) return false;
            }
            return true;
        }
        return oldValue.isJsonPrimitive() && oldValue.getAsJsonPrimitive().isString()
                && newValue.isJsonPrimitive() && newValue.getAsJsonPrimitive().isString()
                && tokens(oldValue.getAsString()).equals(tokens(newValue.getAsString()));
    }

    private static Map<String, Integer> tokens(String text) {
        Map<String, Integer> result = new HashMap<>();
        Matcher matcher = PLACEHOLDER.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            Integer count = result.get(token);
            result.put(token, count == null ? 1 : count + 1);
        }
        return result;
    }
}
