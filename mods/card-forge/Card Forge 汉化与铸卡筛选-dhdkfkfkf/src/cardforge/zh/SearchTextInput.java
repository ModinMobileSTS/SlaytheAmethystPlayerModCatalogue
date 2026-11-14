package cardforge.zh;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import java.nio.charset.StandardCharsets;

/**
 * 搜索框用的键盘处理器。
 *
 * <p>两类来源都支持：</p>
 * <ul>
 *   <li>Amethyst 运行时：调用 {@code Gdx.input.setInputProcessor} 传入“看起来像文本输入”的处理器
 *       （类名含 text/input/type/keyboard，且不含 scroll/mouse/touch）时会请求系统软键盘，
 *       系统输入法的结果会作为按键字符送给当前处理器。</li>
 *   <li>其它安卓后端：由界面调用 {@code Gdx.input.getTextInput} 直接弹出系统输入法对话框。</li>
 * </ul>
 *
 * <p>此外，运行时把整段文本一次性回传时会用 {@code \ue000<base64>\ue001} 包起来
 * （TogetherInSpire 聊天框同样的协议），这里一并解析。</p>
 */
final class SearchTextInput implements InputProcessor {
    /** 单次搜索的最大字符数。 */
    private static final int MAX_LEN = 24;
    private static final char SYNC_START = '\ue000';
    private static final char SYNC_END = '\ue001';
    private static final int SYNC_MAX = 16384;

    interface Listener {
        /** 文本变化。 */
        void changed(String text);

        /** 系统输入法一次性提交了整段文本。 */
        void committed(String text);

        /** 用户确认输入结束。 */
        void done();
    }

    private final StringBuilder buffer = new StringBuilder();
    private final StringBuilder sync = new StringBuilder();
    private final Listener listener;
    private final InputProcessor previous;

    private boolean readingSync;
    private boolean discardSync;

    SearchTextInput(String initial, Listener listener, InputProcessor previous) {
        if (initial != null) {
            buffer.append(initial);
        }
        this.listener = listener;
        this.previous = previous;
    }

    private void backspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            listener.changed(buffer.toString());
        }
    }

    private void append(char character) {
        if (buffer.length() < MAX_LEN) {
            buffer.append(character);
            listener.changed(buffer.toString());
        }
    }

    private static boolean isBase64(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9'
                || character == '+' || character == '/' || character == '=';
    }

    /** 简易 base64 解码（不依赖 android.util，保证桌面/其它加载器同样可用）。 */
    private static String decodeBase64(String text) {
        int[] table = new int[128];
        for (int i = 0; i < table.length; i++) {
            table[i] = -1;
        }
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        for (int i = 0; i < alphabet.length(); i++) {
            table[alphabet.charAt(i)] = i;
        }
        byte[] out = new byte[text.length() * 3 / 4 + 3];
        int length = 0;
        int accumulator = 0;
        int bits = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '=') {
                break;
            }
            int value = c < 128 ? table[c] : -1;
            if (value < 0) {
                continue;
            }
            accumulator = (accumulator << 6) | value;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out[length++] = (byte) ((accumulator >> bits) & 0xFF);
            }
        }
        return new String(out, 0, length, StandardCharsets.UTF_8);
    }

    private void finishSync() {
        String text = null;
        if (!discardSync) {
            try {
                text = decodeBase64(sync.toString());
            } catch (Throwable ignored) {
                text = null;
            }
        }
        sync.setLength(0);
        readingSync = false;
        discardSync = false;
        if (text != null) {
            listener.committed(text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            listener.done();
            return true;
        }
        if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.FORWARD_DEL) {
            backspace();
            return true;
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        if (readingSync) {
            if (character == SYNC_END) {
                finishSync();
                return true;
            }
            if (isBase64(character) && sync.length() < SYNC_MAX) {
                sync.append(character);
            } else {
                sync.setLength(0);
                discardSync = true;
            }
            return true;
        }
        if (character == SYNC_START) {
            sync.setLength(0);
            readingSync = true;
            discardSync = false;
            return true;
        }
        if (character == 8 || character == 127) {
            backspace();
            return true;
        }
        if (character == '\r' || character == '\n') {
            listener.done();
            return true;
        }
        if (character < 32) {
            return true;
        }
        append(character);
        return true;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return previous != null && previous.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return previous != null && previous.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return previous != null && previous.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return previous != null && previous.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(int amount) {
        return previous != null && previous.scrolled(amount);
    }
}
