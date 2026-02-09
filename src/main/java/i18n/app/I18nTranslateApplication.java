package i18n.app;

import static i18n.CommonContext.API_KEY;
import static i18n.CommonContext.EN_OUTPUT;
import static i18n.CommonContext.OUTPUT;
import static i18n.CommonContext.TARGET_LANG;

import com.google.common.collect.Lists;
import i18n.CommonContext;
import i18n.io.PropertiesWrite;
import i18n.scan.PropertiesLoader;
import i18n.zai.ZhipuBatchTranslator;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 国际化文案翻译器
 * @author yao.xiao
 */
public class I18nTranslateApplication {

    private static final int BATCH_SIZE = 500;

    public static void main(String[] args) throws Exception {

        // 1. 读取 properties（保持顺序）
        LinkedHashMap<String, String> source =
            PropertiesLoader.loadOrdered(CommonContext.INPUT);

        // 2. 批量翻译
        ZhipuBatchTranslator translator =
            new ZhipuBatchTranslator(API_KEY, TARGET_LANG);

//        Properties output = new Properties();
        Map<String, String> output = new LinkedHashMap<>();
        List<Map.Entry<String, String>> entries = new ArrayList<>(source.entrySet());

        batchTranslate(entries, translator , output , BATCH_SIZE);
    }

    private static void batchTranslate(List<Entry<String, String>> entries, ZhipuBatchTranslator translator , Map<String, String> output , int batchSize) throws IOException {
        List<List<Entry<String, String>>> partitions = Lists.partition(entries, batchSize);
        for (List<Entry<String, String>> partition : partitions) {


            List<String> chinese = partition.stream()
                    .map(Entry::getValue)
                    .collect(Collectors.toList());

            List<String> translated = translator.translate(chinese);

            System.out.println("translated size: " + translated.size() + " === Chinese size: " + chinese.size());

            if (translated.size() != chinese.size()) {
                System.out.println("==============存在行数不一致，进入细分批次处理============");
                batchSize = batchSize/2;
                batchTranslate(partition, translator, output , batchSize);
                batchSize = BATCH_SIZE;
                continue;
            }

            LinkedHashMap<String, String> result = new LinkedHashMap<>();
            for (int j = 0; j < partition.size(); j++) {
                result.put(partition.get(j).getKey(), translated.get(j));
            }
            // 4. 写出文件
            write(Paths.get(OUTPUT), result , output);
        }
    }

    public static void write(Path path,
        LinkedHashMap<String, String> data , Map<String, String> output )
        throws IOException {

        // 3️⃣ 一一对应写入
        for (Entry<String, String> entry : data.entrySet()) {
            output.put(entry.getKey(), entry.getValue());
            System.out.println(entry.getKey() + "=" + entry.getValue());
        }

        PropertiesWrite.writeProperties(EN_OUTPUT, output);

//        Files.createDirectories(path.getParent());

        /*try (Writer writer = Files.newBufferedWriter(path)) {
            output.store(writer, "auto generated i18n");
        }*/
    }
}
