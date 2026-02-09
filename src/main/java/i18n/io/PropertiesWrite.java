package i18n.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class PropertiesWrite {

    public static void writeProperties(String output, Map<String, String> output1) throws IOException {
        Path outputPath = Paths.get(output);
        Files.createDirectories(outputPath.getParent());
        // 所有 batch 处理完之后
        writeOrderedProperties(output1, outputPath);
    }

    public static void writeOrderedProperties(
        Map<String, String> output,
        Path outputPath
    ) throws IOException {

        Files.createDirectories(outputPath.getParent());

        List<Entry<String, String>> ordered =
            output.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toList());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {

            writer.write("# auto generated i18n");
            writer.newLine();
            writer.newLine();

            String lastPrefix = null;

            for (Map.Entry<String, String> e : ordered) {

                String key = e.getKey();
                String value = e.getValue();

                // ⭐ 用 class 作为分组（可读性极强）
                String prefix = key.substring(0, key.indexOf('.'));

                if (!prefix.equals(lastPrefix)) {
                    /*writer.newLine();
                    writer.write("# " + prefix);*/
                    writer.newLine();
                    lastPrefix = prefix;
                }

                writer.write(key + "=" + value);
                writer.newLine();
            }
        }
    }
}
