package i18n.scan;

import java.util.UUID;

public final class GlobalKeyGenerator {

    private GlobalKeyGenerator() {}

    /**
     * 生成全局唯一 key（8 位，小写字母 + 数字）
     */
    public static String generate() {
        long v = UUID.randomUUID()
            .getMostSignificantBits() & Long.MAX_VALUE;

        // base36：0-9a-z
        String s = Long.toString(v, 36);
        // 保证长度稳定
        return "i18n_"+(s.length() > 8 ? s.substring(0, 8) : s);
    }
}

