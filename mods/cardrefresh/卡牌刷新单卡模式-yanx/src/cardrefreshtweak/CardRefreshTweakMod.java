package cardrefreshtweak;

import basemod.BaseMod;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point. Registers the mod badge in the main menu, whose click opens the settings panel
 * (refresh mode, refresh count and whether loading a save gives the refreshes back).
 */
@SpireInitializer
public class CardRefreshTweakMod implements PostInitializeSubscriber {

    private static final Logger LOG = LogManager.getLogger("CardRefreshTweak");

    private static RefreshConfig settings = null;

    public CardRefreshTweakMod() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new CardRefreshTweakMod();
    }

    @Override
    public void receivePostInitialize() {
        try {
            settings = new RefreshConfig();
        } catch (Throwable t) {
            LOG.error("[CardRefresh] cannot load settings, defaults are used: " + t);
        }
        try {
            BaseMod.registerModBadge(buildBadgeTexture(), "卡牌刷新单卡模式", "AI 补丁",
                    "点击打开设置：单卡刷新模式（关闭 = 整组刷新）、每张卡刷新次数 0~3（0 = 不刷新）、"
                            + "读档后是否返还已用刷新次数（默认返还）。",
                    settings);
            LOG.info("[CardRefresh] patch mod ready, singleCardMode=" + RefreshConfig.singleCardMode
                    + " refreshes=" + RefreshConfig.refreshesPerCard()
                    + " refundOnReload=" + RefreshConfig.refundOnReload);
        } catch (Throwable t) {
            LOG.error("[CardRefresh] cannot register the settings badge: " + t);
        }
    }

    private static Texture buildBadgeTexture() {
        Pixmap pixmap = new Pixmap(40, 40, Pixmap.Format.RGBA8888);
        try {
            pixmap.setColor(0.12f, 0.19f, 0.33f, 1.0f);
            pixmap.fill();
            pixmap.setColor(0.95f, 0.80f, 0.34f, 1.0f);
            pixmap.drawRectangle(0, 0, 40, 40);
            pixmap.drawCircle(20, 20, 12);
            pixmap.drawCircle(20, 20, 11);
            pixmap.fillTriangle(32, 20, 23, 27, 23, 13);
            pixmap.setColor(0.12f, 0.19f, 0.33f, 1.0f);
            pixmap.fillCircle(20, 20, 8);
            pixmap.setColor(0.95f, 0.80f, 0.34f, 1.0f);
            pixmap.fillCircle(20, 20, 4);
            return new Texture(pixmap);
        } finally {
            pixmap.dispose();
        }
    }
}
