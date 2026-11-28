package yuaifix;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.megacrit.cardcrawl.core.Settings;
import yuaimod.YuAiMusic;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

/** Streams the parent mod's built-in track; each open is a fresh attempt. */
public final class Music {
    private static final float MAX_VOLUME = 2.0f;

    private static com.badlogic.gdx.audio.Music clip;
    private static String loadedPath;
    private static float savedPosition;
    private static long lastFailure;

    private Music() {
    }

    public static synchronized boolean play(boolean resume) {
        File file = trackFile();
        if (file == null) {
            return false;
        }
        try {
            if (clip == null || !file.getAbsolutePath().equals(loadedPath)) {
                dispose();
                clip = Gdx.audio.newMusic(new com.badlogic.gdx.files.FileHandle(file));
                if (clip == null) {
                    throw new IllegalStateException("no audio backend");
                }
                clip.setLooping(true);
                loadedPath = file.getAbsolutePath();
            }
            applyVolume();
            clip.play();
            if (resume && savedPosition > 0.0f) {
                try {
                    clip.setPosition(savedPosition);
                } catch (Throwable ignored) {
                }
            }
            savedPosition = 0.0f;
            return true;
        } catch (Throwable t) {
            long now = System.currentTimeMillis();
            if (now - lastFailure > 5000L) {
                lastFailure = now;
                System.err.println("[雨爱修复] 播放失败，稍后重试: " + t);
            }
            dispose();
            return false;
        }
    }

    public static synchronized void pause() {
        if (clip == null) {
            return;
        }
        try {
            savedPosition = Math.max(0.0f, clip.getPosition());
            clip.pause();
        } catch (Throwable t) {
            savedPosition = 0.0f;
        }
    }

    public static synchronized void stop() {
        dispose();
        savedPosition = 0.0f;
    }

    /** Used on a new run so the previous run's position is never resumed. */
    public static synchronized void discard() {
        dispose();
        savedPosition = 0.0f;
    }

    public static synchronized boolean playing() {
        if (clip == null) {
            return false;
        }
        try {
            return clip.isPlaying();
        } catch (Throwable t) {
            return false;
        }
    }

    public static synchronized void syncVolume() {
        applyVolume();
    }

    private static void applyVolume() {
        if (clip == null) {
            return;
        }
        float musicVolume = Settings.MUSIC_VOLUME;
        float masterVolume = Settings.MASTER_VOLUME;
        if (Float.isNaN(musicVolume)) {
            musicVolume = 0.5f;
        }
        if (Float.isNaN(masterVolume)) {
            masterVolume = 0.5f;
        }
        musicVolume = Math.max(0.0f, Math.min(MAX_VOLUME, musicVolume));
        masterVolume = Math.max(0.0f, Math.min(MAX_VOLUME, masterVolume));
        try {
            clip.setVolume(Config.volume() * musicVolume * masterVolume);
        } catch (Throwable ignored) {
        }
    }

    private static File trackFile() {
        String name = "yuaifix-track.wav";
        FileHandle[] targets = trackTargets(name);
        for (int i = 0; i < targets.length; i++) {
            if (targets[i].exists() && targets[i].length() > 0L) {
                return targets[i].file();
            }
        }
        InputStream in = YuAiMusic.class.getResourceAsStream("/yuaimod/audio/yuai.wav");
        if (in == null) {
            return null;
        }
        byte[] data;
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[16384];
            int read;
            while ((read = in.read(chunk)) > 0) {
                buffer.write(chunk, 0, read);
            }
            data = buffer.toByteArray();
        } catch (Throwable t) {
            return null;
        } finally {
            try {
                in.close();
            } catch (Throwable ignored) {
            }
        }
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].writeBytes(data, false);
                if (targets[i].length() > 0L) {
                    return targets[i].file();
                }
            } catch (Throwable ignored) {
                try {
                    targets[i].delete();
                } catch (Throwable suppressed) {
                }
            }
        }
        return null;
    }

    private static FileHandle[] trackTargets(String name) {
        FileHandle local = null;
        try {
            if (Gdx.files != null) {
                local = Gdx.files.local(name);
            }
        } catch (Throwable ignored) {
        }
        FileHandle user = new FileHandle(new File(System.getProperty("user.dir", "."), name));
        FileHandle temp = new FileHandle(new File(System.getProperty("java.io.tmpdir", "."), name));
        if (local == null) {
            return new FileHandle[]{user, temp};
        }
        return new FileHandle[]{local, user, temp};
    }

    private static void dispose() {
        if (clip == null) {
            return;
        }
        try {
            clip.stop();
        } catch (Throwable ignored) {
        }
        try {
            clip.dispose();
        } catch (Throwable ignored) {
        }
        clip = null;
        loadedPath = null;
    }
}
