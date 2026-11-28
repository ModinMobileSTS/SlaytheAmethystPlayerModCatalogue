package yuaifix;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/** Persists the track volume factor; default 0.85 (85%). */
public final class Config {
    public static final float MAX_VOLUME = 2.0f;

    private static final String FILE_NAME = "yuaifix-config.properties";
    private static final String KEY = "trackVolume";
    private static final float DEFAULT = 0.85f;
    private static final float OLD_DEFAULT = 1.2f;

    private static volatile float volume = load();

    private Config() {
    }

    public static float volume() {
        return volume;
    }

    public static synchronized void setVolume(float value) {
        if (Float.isNaN(value)) {
            return;
        }
        float clamped = Math.max(0.0f, Math.min(MAX_VOLUME, value));
        if (Math.abs(clamped - volume) < 0.0001f) {
            return;
        }
        volume = clamped;
        save();
    }

    private static File[] files() {
        return new File[]{
                new File(System.getProperty("user.dir", "."), FILE_NAME),
                new File(System.getProperty("java.io.tmpdir", "."), FILE_NAME)
        };
    }

    private static float load() {
        File best = null;
        File[] files = files();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isFile() && (best == null || file.lastModified() > best.lastModified())) {
                best = file;
            }
        }
        if (best == null) {
            return DEFAULT;
        }
        try {
            Properties properties = new Properties();
            FileInputStream in = new FileInputStream(best);
            try {
                properties.load(in);
            } finally {
                in.close();
            }
            float value = Float.parseFloat(properties.getProperty(KEY, Float.toString(DEFAULT)));
            if (Float.isNaN(value)) {
                return DEFAULT;
            }
            if (Math.abs(value - OLD_DEFAULT) < 0.0001f) {
                return DEFAULT;
            }
            return Math.max(0.0f, Math.min(MAX_VOLUME, value));
        } catch (Throwable t) {
            return DEFAULT;
        }
    }

    private static void save() {
        File[] files = files();
        for (int i = 0; i < files.length; i++) {
            try {
                Properties properties = new Properties();
                properties.setProperty(KEY, Float.toString(volume));
                FileOutputStream out = new FileOutputStream(files[i]);
                try {
                    properties.store(out, "YuAi track volume");
                } finally {
                    out.close();
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
