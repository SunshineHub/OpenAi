package i18n.scan;


import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;

public class PropertiesLoader {

    public static LinkedHashMap<String, String> loadOrdered(String path)
        throws Exception {

        LinkedHashMap<String, String> map = new LinkedHashMap<>();

        try (BufferedReader reader =
            Files.newBufferedReader(Paths.get(path))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int idx = line.indexOf('=');
                if (idx < 0) continue;

                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1);

                map.put(key, value);
            }
        }
        return map;
    }
}

