package neowpreview;

import basemod.BaseMod;
import basemod.ModLabeledButton;
import basemod.ModPanel;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;

/**
 * 在 BaseMod 的模组设置面板里（面板框内靠左）提供“图片大小”滑条、“涅奥始终有四个选项”开关，
 * 以及一个“重置为默认”按钮。100% 即原版大小，可调 50%-200%。
 */
@SpireInitializer
public class NeowPreviewInit implements PostInitializeSubscriber {

    /** 面板框从左约 278*scale 开始，这里留足边距，保证标签和滑条都在框内。 */
    private static final float SLIDER_X = 540.0f;
    private static final float SLIDER_IMAGE_Y = 680.0f;
    private static final float TOGGLE_X = 400.0f;
    private static final float TOGGLE_Y = 560.0f;
    private static final float RESET_X = 500.0f;
    private static final float RESET_Y = 460.0f;

    public NeowPreviewInit() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new NeowPreviewInit();
    }

    @Override
    public void receivePostInitialize() {
        Config.load();
        try {
            ModPanel panel = new ModPanel();

            PercentSlider image = new PercentSlider(Lang.sliderImage(), SLIDER_X, SLIDER_IMAGE_Y,
                    Config.MIN_PERCENT, Config.MAX_PERCENT, 100, Config.imagePercent,
                    Config::setImagePercent);
            panel.addUIElement(image);

            ConfigToggle four = new ConfigToggle(Lang.alwaysFour(), TOGGLE_X, TOGGLE_Y,
                    Config.alwaysFour(), Config::setAlwaysFour);
            panel.addUIElement(four);

            ModLabeledButton reset = new ModLabeledButton(Lang.resetDefault(), RESET_X, RESET_Y,
                    panel, button -> {
                image.setPercent(Config.DEFAULT_IMAGE);
                Config.setImagePercent(Config.DEFAULT_IMAGE);
                four.setValue(Config.DEFAULT_ALWAYS_FOUR);
                Config.setAlwaysFour(Config.DEFAULT_ALWAYS_FOUR);
            });
            panel.addUIElement(reset);

            BaseMod.registerModBadge(makeBadge(), Lang.modName(), "AI patch", Lang.modDesc(), panel);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 徽章不显示图标，只保留原版徽章位大小（40x40）的透明贴图，
     * 这样既不会和其它模组图标重叠，又还能点开设置面板（鼠标悬停有说明）。
     */
    private static Texture makeBadge() {
        Pixmap pixmap = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.0f, 0.0f, 0.0f, 0.0f));
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return texture;
    }
}
