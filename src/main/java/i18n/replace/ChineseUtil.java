package i18n.replace;

public final class ChineseUtil {

    private ChineseUtil() {}

    /**
     * 判断字符串是否包含中文字符
     */
    public static boolean containsChinese(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (isChinese(c)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字符是否是中文（覆盖常见汉字区）
     */
    private static boolean isChinese(char c) {
        return c >= '\u4E00' && c <= '\u9FFF';
    }
}

