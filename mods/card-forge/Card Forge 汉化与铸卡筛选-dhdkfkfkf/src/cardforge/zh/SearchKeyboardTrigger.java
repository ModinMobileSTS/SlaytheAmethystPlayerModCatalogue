package cardforge.zh;

import com.badlogic.gdx.InputProcessor;

/**
 * 只为唤起系统输入法而临时装上的占位处理器。
 *
 * <p>运行时在 {@code setInputProcessor} 时，如果处理器类名含 text/input/type/keyboard
 * （且不含 scroll/mouse/touch），就会写一次键盘请求文件并弹出系统输入法。
 * 这里用一个名字符合该规则的极简处理器触发它，函数返回前就把 {@link CraftPointer} 装回去，
 * 于是字符仍由常驻处理器接收，不会因为焦点变化丢字。</p>
 */
final class SearchKeyboardTrigger implements InputProcessor {
    private final CraftPointer target;

    SearchKeyboardTrigger(CraftPointer target) {
        this.target = target;
    }

    /** 类名命中运行时规则，用于触发键盘请求；不转发触摸，避免重复处理。 */
    @Override
    public boolean keyDown(int keycode) {
        return target != null && target.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return target != null && target.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        return target != null && target.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }
}
