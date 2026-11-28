package yuaifix;

import basemod.BaseMod;
import basemod.ModPanel;
import basemod.ModSlider;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/** Badge panel with a single volume slider (0-200%). */
@SpireInitializer
public final class VolumePanel implements PostInitializeSubscriber {
    public VolumePanel() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new VolumePanel();
    }

    @Override
    public void receivePostInitialize() {
        ModPanel panel = new ModPanel();
        ModSlider slider = new ModSlider("音量", 480.0f, 720.0f,
                Config.MAX_VOLUME * 100.0f, "%", panel, source -> {
                    Config.setVolume(source.value * Config.MAX_VOLUME);
                    Music.syncVolume();
                });
        slider.setValue(Config.volume() / Config.MAX_VOLUME);
        panel.addUIElement(slider);
        BaseMod.registerModBadge(badgeTexture(), "雨爱音量", "音量设置",
                "曲目音量 0~200%", panel);
    }

    private static Texture badgeTexture() {
        Pixmap pixmap = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.30f, 0.62f, 0.92f, 1.0f));
        pixmap.fill();
        pixmap.setColor(Color.WHITE);
        pixmap.drawRectangle(0, 0, 40, 40);
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }
}
