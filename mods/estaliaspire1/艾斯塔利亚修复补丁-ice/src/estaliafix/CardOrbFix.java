package estaliafix;

import basemod.abstracts.CustomCard;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch2;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.megacrit.cardcrawl.cards.AbstractCard;

/**
 * 修复 Estalia 卡牌费用球贴图尺寸不符合 BaseMod 卡牌图集约定，导致圆球被画在卡面中央。
 * 原版小卡费用球位于 512x512 卡牌画布的左上费用区域；这里保留父模组圆球内容，
 * 只补上对应的 AtlasRegion 原图尺寸、偏移和目标尺寸。
 */
public class CardOrbFix {

    @SpirePatch2(
            clz = basemod.patches.com.megacrit.cardcrawl.cards.AbstractCard.RenderFixSwitches.RenderEnergySwitch.class,
            method = "getEnergyOrb",
            paramtypez = {AbstractCard.class, TextureAtlas.AtlasRegion.class}
    )
    public static class EnergyOrbRegion {

        @SpirePrefixPatch
        public static SpireReturn prefix(AbstractCard card, TextureAtlas.AtlasRegion orb) {
            if (!(card instanceof estalia.Card)) {
                return SpireReturn.Continue();
            }

            Texture texture = ((CustomCard) card).getOrbSmallTexture();
            if (texture == null) {
                return SpireReturn.Continue();
            }

            TextureAtlas.AtlasRegion region = new TextureAtlas.AtlasRegion(
                    texture, 0, 0, texture.getWidth(), texture.getHeight());
            // Values match cardui.atlas 512/card_blue_orb.
            region.originalWidth = 512;
            region.originalHeight = 512;
            region.offsetX = 90.0f;
            region.offsetY = 415.0f;
            region.packedWidth = 67;
            region.packedHeight = 67;
            return SpireReturn.Return(region);
        }
    }
}
