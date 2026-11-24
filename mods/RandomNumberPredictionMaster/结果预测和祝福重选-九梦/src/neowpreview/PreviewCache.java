package neowpreview;

import java.util.function.Supplier;

/**
 * 单个预览结果的小缓存：悬停期间状态没变就不再重算。
 *
 * <p>预览里的推算要复制随机数、洗牌、翻卡池，每帧都做一次是浪费；
 * 这里按「事件实例 + 状态串」缓存，鼠标移开或状态一变（换页、掉血、换选项）才重算。</p>
 */
public final class PreviewCache {

    /** 提示框类预览。 */
    public static final PreviewCache TIPS = new PreviewCache();
    /** 卡面类预览。 */
    public static final PreviewCache CARDS = new PreviewCache();

    private Object owner;
    private String key;
    private Object value;

    private PreviewCache() {
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Object who, String newKey, Supplier<T> supplier) {
        if (who != null && newKey != null && who == this.owner && newKey.equals(this.key) && this.value != null) {
            return (T) this.value;
        }
        T computed = supplier.get();
        this.owner = who;
        this.key = newKey;
        this.value = computed;
        return computed;
    }

    public void clear() {
        this.owner = null;
        this.key = null;
        this.value = null;
    }
}
