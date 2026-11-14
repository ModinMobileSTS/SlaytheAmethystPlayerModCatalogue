package cardforge.zh;

import com.badlogic.gdx.Input;
import java.nio.charset.StandardCharsets;

/**
 * 搜索词的文本缓冲（不是输入处理器）。
 *
 * <p>常驻的 {@link CraftPointer} 已接管触摸与按键，这里只负责把字符累积成搜索词，
 * 因此不需要再来回替换 {@code Gdx.input} 的处理器，也不会因为失焦被换掉而丢字。</p>
 *
 * <p>两类来源都支持：</p>
 * <ul>
 *   <li>运行时把系统输入法的结果逐字送进 {@code keyTyped}；</li>
 *   <li>运行时把整段文本一次性回传时用 {@code \ue000<base64>\ue001} 包起来
 *       （TogetherInSpire 聊天框同样的协议），这里一并解析。</li>
 * </ul>
 *
 * <p>界面上还有自己的“退格 / 清除”按钮：加载器那个键盘预览条里的文字删不掉，
 * 所以删字一律在游戏里做（见 {@link #reset(String)} 把按钮改动同步回缓冲）。</p>
 */
final class SearchTextInput {
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

    private boolean readingSync;
    private boolean discardSync;

    SearchTextInput(String initial, Listener listener) {
        reset(initial);
        this.listener = listener;
    }

    /** 用外部值替换当前内容（界面上的退格 / 清除按钮用）。 */
    void reset(String text) {
        buffer.setLength(0);
        if (text != null) {
            buffer.append(text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text);
        }
    }

    private void backspace() {
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);
            if (listener != null) {
                listener.changed(buffer.toString());
            }
        }
    }

    private void append(char character) {
        if (buffer.length() < MAX_LEN) {
            buffer.append(character);
            if (listener != null) {
                listener.changed(buffer.toString());
            }
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
            reset(text);
            if (listener != null) {
                listener.committed(text.length() > MAX_LEN ? text.substring(0, MAX_LEN) : text);
            }
        }
    }

    /** 按键按下：回车/返回结束输入，退格删字。 */
    void handleKeyDown(int keycode) {
        if (keycode == Input.Keys.ENTER || keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            if (listener != null) {
                listener.done();
            }
            return;
        }
        if (keycode == Input.Keys.BACKSPACE || keycode == Input.Keys.FORWARD_DEL) {
            backspace();
        }
    }

    /** 按键字符：普通字符累积，{@code \ue000…\ue001} 视为整段提交。 */
    void handleKeyTyped(char character) {
        if (readingSync) {
            if (character == SYNC_END) {
                finishSync();
                return;
            }
            if (isBase64(character) && sync.length() < SYNC_MAX) {
                sync.append(character);
            } else {
                sync.setLength(0);
                discardSync = true;
            }
            return;
        }
        if (character == SYNC_START) {
            sync.setLength(0);
            readingSync = true;
            discardSync = false;
            return;
        }
        if (character == 8 || character == 127) {
            backspace();
            return;
        }
        if (character == '\r' || character == '\n') {
            if (listener != null) {
                listener.done();
            }
            return;
        }
        if (character < 32) {
            return;
        }
        append(character);
    }
}
