package i18n.replace;


import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class I18nIndex {

    /**
     * 中文 -> List<完整key>
     */
    private final Map<String, List<String>> zhToKeys = new HashMap<>();

    public static I18nIndex load(Path propertiesPath) throws Exception {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(propertiesPath)) {
            p.load(r);
        }

        I18nIndex index = new I18nIndex();

        for (String key : p.stringPropertyNames()) {
            String zh = p.getProperty(key);
            index.zhToKeys
                .computeIfAbsent(zh, k -> new ArrayList<>())
                .add(key);
        }
        return index;
    }

    /**
     * 根据 classFqn + 中文，找到最匹配的 key
     */
    public Optional<String> findKey(String classFqn, String chinese) {
        List<String> keys = zhToKeys.get(chinese);
        if (keys == null) return Optional.empty();

        return keys.stream()
            .filter(k -> k.startsWith(classFqn))
            .findFirst()
            .or(Optional::empty);
    }
}

