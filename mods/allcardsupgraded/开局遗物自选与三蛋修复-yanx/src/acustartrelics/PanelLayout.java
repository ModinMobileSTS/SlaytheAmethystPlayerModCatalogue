package acustartrelics;

import com.megacrit.cardcrawl.core.Settings;

/**
 * BaseMod 的 ModPanel 背景在 ModPanel.renderBg() 里是写死公式画的：
 * sb.draw(background, 278 * scale, OPTION_Y - 376, 1364 * 752 * scale)。
 * 这里用同一套数值换算出「面板图片」在控件坐标系里的位置，
 * 所有元素都按图片内的相对像素摆放，因此不同分辨率/缩放下不会跑偏。
 */
public final class PanelLayout {

    /** 面板图片尺寸（像素）。 */
    public static final float IMG_W = 1364f;
    public static final float IMG_H = 752f;

    /** 内容相对图片边缘的内缩，避开背景图的装饰边框。 */
    public static final float INSET = 40f;

    private static final float ORIGIN_X = 278f;
    private static final float ORIGIN_Y_OFFSET = 376f;

    private PanelLayout() {
    }

    /** 把「面板图片内的 x」转换成控件使用的设计坐标（控件内部会再乘 Settings.scale）。 */
    public static float x(float imgX) {
        return ORIGIN_X + imgX;
    }

    /** 把「面板图片内的 y」转换成控件使用的设计坐标。 */
    public static float y(float imgY) {
        return (Settings.OPTION_Y - ORIGIN_Y_OFFSET) / Settings.scale + imgY;
    }
}
