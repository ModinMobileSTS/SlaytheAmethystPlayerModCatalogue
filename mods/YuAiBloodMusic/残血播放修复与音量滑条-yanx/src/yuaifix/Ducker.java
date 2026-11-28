package yuaifix;

import com.badlogic.gdx.audio.Music;
import com.megacrit.cardcrawl.audio.MainMusic;
import com.megacrit.cardcrawl.audio.MusicMaster;
import com.megacrit.cardcrawl.audio.TempMusic;
import com.megacrit.cardcrawl.core.CardCrawlGame;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Retryable BGM ducking with an immediate restore pass when it is switched off. */
public final class Ducker {
    private static final float DUCK_FACTOR = 0.2f;
    private static final float EPS = 1.0e-4f;
    private static final long RETRY_DELAY = 5000L;

    private static final Map<Music, State> STATE = new IdentityHashMap<Music, State>();
    private static boolean ducked;
    private static boolean broken;
    private static long brokenUntil;
    private static long lastError;
    private static Field mainTrackField;
    private static Field tempTrackField;
    private static Field mainMusicField;
    private static Field tempMusicField;

    private Ducker() {
    }

    private static final class State {
        private final float original;
        private float applied;

        private State(float volume) {
            this.original = volume;
            this.applied = volume;
        }
    }

    public static synchronized void set(boolean value) {
        if (ducked == value) {
            return;
        }
        ducked = value;
        if (!ducked) {
            tick(true);
        }
    }

    public static synchronized void tick() {
        tick(false);
    }

    private static synchronized void tick(boolean force) {
        if (!ducked && STATE.isEmpty() && !force) {
            return;
        }
        long now = System.currentTimeMillis();
        if (broken && now < brokenUntil) {
            return;
        }
        try {
            if (!ensureFields()) {
                return;
            }
            MusicMaster master = CardCrawlGame.music;
            if (master == null) {
                return;
            }
            Set<Music> seen = Collections.newSetFromMap(new IdentityHashMap<Music, Boolean>());
            applyList(mainTrackField.get(master), mainMusicField, seen);
            applyList(tempTrackField.get(master), tempMusicField, seen);
            Iterator<Music> iterator = STATE.keySet().iterator();
            while (iterator.hasNext()) {
                if (!seen.contains(iterator.next())) {
                    iterator.remove();
                }
            }
            broken = false;
        } catch (Throwable t) {
            broken = true;
            brokenUntil = System.currentTimeMillis() + RETRY_DELAY;
            if (now - lastError > 30000L) {
                lastError = now;
                System.err.println("[雨爱修复] BGM 音量控制异常，稍后重试: " + t);
            }
        }
    }

    private static void applyList(Object value, Field musicField, Set<Music> seen)
            throws IllegalAccessException {
        if (!(value instanceof List)) {
            return;
        }
        for (Object entry : (List<?>) value) {
            if (entry == null) {
                continue;
            }
            Object raw = musicField.get(entry);
            if (!(raw instanceof Music)) {
                continue;
            }
            Music track = (Music) raw;
            seen.add(track);
            float current = volume(track);
            State state = STATE.get(track);
            if (ducked) {
                if (state == null) {
                    state = new State(current);
                    STATE.put(track, state);
                } else if (Math.abs(current - state.applied) > EPS) {
                    state = new State(current);
                    STATE.put(track, state);
                }
                float target = state.original * DUCK_FACTOR;
                if (Math.abs(target - current) > EPS) {
                    setVolume(track, target);
                }
                state.applied = target;
            } else if (state != null) {
                if (Math.abs(current - state.applied) <= EPS) {
                    setVolume(track, state.original);
                }
                STATE.remove(track);
            }
        }
    }

    private static float volume(Music track) {
        try {
            float value = track.getVolume();
            return Float.isNaN(value) ? 0.0f : value;
        } catch (Throwable t) {
            return 0.0f;
        }
    }

    private static void setVolume(Music track, float value) {
        try {
            track.setVolume(value);
        } catch (Throwable ignored) {
        }
    }

    private static boolean ensureFields() throws NoSuchFieldException {
        if (mainTrackField != null && tempTrackField != null
                && mainMusicField != null && tempMusicField != null) {
            return true;
        }
        mainTrackField = MusicMaster.class.getDeclaredField("mainTrack");
        tempTrackField = MusicMaster.class.getDeclaredField("tempTrack");
        mainMusicField = MainMusic.class.getDeclaredField("music");
        tempMusicField = TempMusic.class.getDeclaredField("music");
        mainTrackField.setAccessible(true);
        tempTrackField.setAccessible(true);
        mainMusicField.setAccessible(true);
        tempMusicField.setAccessible(true);
        return true;
    }
}
