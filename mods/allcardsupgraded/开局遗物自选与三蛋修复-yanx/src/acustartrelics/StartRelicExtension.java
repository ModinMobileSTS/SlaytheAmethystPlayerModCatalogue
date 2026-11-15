package acustartrelics;

import basemod.BaseMod;
import basemod.ModLabel;
import basemod.ModPanel;
import basemod.ModToggleButton;
import basemod.interfaces.PostDungeonInitializeSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.helpers.RelicLibrary;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 主菜单 mod 设置里的「开局遗物自选」：注册徽章 + 自绘面板，并在每局开局发放所选遗物。
 * 徽章图标借用了 Loadout Mod 的资源（已复制进本补丁），运行时不依赖 Loadout。
 */
@SpireInitializer
public class StartRelicExtension implements PostInitializeSubscriber, PostDungeonInitializeSubscriber {

    private static final Logger logger = LogManager.getLogger("StartRelicExtension");
    private static final String BADGE_IMG = "acustartrelics/images/badge.png";

    public StartRelicExtension() {
        BaseMod.subscribe(this);
    }

    public static void initialize() {
        new StartRelicExtension();
    }

    @Override
    public void receivePostInitialize() {
        StartRelicSettings.init();
        try {
            ModPanel panel = new ModPanel();
            panel.addUIElement(new ModLabel(Texts.TITLE, 330f, 852f, Color.WHITE, panel, me -> {
            }));
            panel.addUIElement(new ModLabel(Texts.HINT, 330f, 818f, Color.LIGHT_GRAY, panel, me -> {
            }));
            panel.addUIElement(new RelicPickerElement(panel));

            ModToggleButton toggle = new ModToggleButton(330f, 240f, StartRelicSettings.isEnabled(), true, panel,
                    button -> StartRelicSettings.setEnabled(button.enabled));
            panel.addUIElement(toggle);
            panel.addUIElement(new ModLabel(Texts.TOGGLE, 396f, 252f, Color.WHITE, panel, me -> {
            }));

            Texture badge = ImageMaster.loadImage(BADGE_IMG);
            BaseMod.registerModBadge(badge, Texts.BADGE_NAME, Texts.AUTHOR, Texts.BADGE_DESC, panel);
        } catch (Exception e) {
            logger.error("Failed to register start relic settings panel", e);
        }
    }

    @Override
    public void receivePostDungeonInitialize() {
        if (!StartRelicSettings.isEnabled()) {
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        if (player == null) {
            return;
        }
        // 只在开局发放，读档 / 换层不会重复给
        if (AbstractDungeon.floorNum > 0) {
            return;
        }
        List<String> ids = StartRelicSettings.getSelectedIds();
        if (ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            AbstractRelic proto = RelicLibrary.getRelic(id);
            if (proto == null || !id.equals(proto.relicId)) {
                // 未知 id：RelicLibrary 会返回 Circlet，这里直接跳过
                logger.warn("Unknown relic id in config: " + id);
                continue;
            }
            if (player.hasRelic(id)) {
                continue;
            }
            proto.makeCopy().instantObtain();
            removeFromRelicPools(id);
            logger.info("Granted starting relic: " + id);
        }
    }

    private static void removeFromRelicPools(String relicId) {
        removeAll(AbstractDungeon.commonRelicPool, relicId);
        removeAll(AbstractDungeon.uncommonRelicPool, relicId);
        removeAll(AbstractDungeon.rareRelicPool, relicId);
        removeAll(AbstractDungeon.shopRelicPool, relicId);
        removeAll(AbstractDungeon.bossRelicPool, relicId);
    }

    private static void removeAll(ArrayList<String> pool, String relicId) {
        if (pool == null) {
            return;
        }
        for (int i = pool.size() - 1; i >= 0; i--) {
            if (relicId.equals(pool.get(i))) {
                pool.remove(i);
            }
        }
    }
}
