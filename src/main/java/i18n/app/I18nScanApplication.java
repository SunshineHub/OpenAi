package i18n.app;

import static i18n.CommonContext.API_KEY;
import static i18n.CommonContext.BATCH_SIZE;
import static i18n.CommonContext.ENUM_FILE_EXIST;
import static i18n.CommonContext.OUTPUT;
import static i18n.CommonContext.OUTPUT_ENUMS;
import static i18n.CommonContext.PROJECT_ROOT;
import static i18n.CommonContext.SIMPLE_MODE;

import com.google.common.collect.Lists;
import i18n.io.PropertiesWrite;
import i18n.scan.GlobalKeyGenerator;
import i18n.zai.I18nText;
import i18n.scan.JavaSourceScanner;
import i18n.zai.ZhipuBatchKeyGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 国际化文案扫描器
 *
 * @author yao.xiao
 */
public class I18nScanApplication {


    public static void main(String[] args) throws Exception {

    }


    public static void classScan(Path projectRoot, Map<String,String> output , boolean simpleMode) throws IOException, InterruptedException {

        JavaSourceScanner scanner = new JavaSourceScanner();
        List<I18nText> texts = scanner.scan(projectRoot);
        System.out.println("texts total size: " + texts.size());

        // 简单模式，不翻译最后一截key,直接随机生成字符
        if (simpleMode) {
            simpleModeScan(output, texts);
            return;
        }

        ZhipuBatchKeyGenerator batchGenerator =
            new ZhipuBatchKeyGenerator(API_KEY);
        // 每批 xx 条（你可以自己调）
        handleBatch(texts, batchGenerator, output, BATCH_SIZE);
    }

    public static void simpleModeScan(Map<String, String> output, List<I18nText> texts) throws IOException {
        for (I18nText text : texts) {

            String key = GlobalKeyGenerator.generate();
            String fullKey = text.getOwnerFqn() + "." + key;

            // 极低概率冲突兜底
            while (output.containsKey(fullKey)) {
                fullKey = text.getOwnerFqn() + "." + GlobalKeyGenerator.generate();
            }

            output.put(fullKey, text.getChinese());
            System.out.println(fullKey + "=" + text.getChinese());
        }

        PropertiesWrite.writeProperties(OUTPUT, output);
        /*try (Writer writer = Files.newBufferedWriter(outputPath)) {
            output.store(writer, "auto generated i18n");
        }*/
    }


    public static void enumScan(Path projectRoot, Map<String, String> output) throws IOException {
        JavaSourceScanner enumsScanner = new JavaSourceScanner();
        List<I18nText> enums = enumsScanner.scanEnums(projectRoot);
        System.out.println("enums total size: " + enums.size());
        int index = 2;
        for (I18nText enumFile : enums) {
            // 一一对应写入
            String fullKey = enumFile.getOwnerFqn();

            if (output.containsKey(fullKey)) {
                fullKey = fullKey + "." + index;
                index++;
            }
            output.put(fullKey, enumFile.getChinese());

            System.out.println(fullKey + "=" + enumFile.getChinese());
        }
        PropertiesWrite.writeProperties(OUTPUT_ENUMS, output);
        /*try (Writer writer = Files.newBufferedWriter(outputPath)) {
            output.store(writer, "auto generated i18n");
        }*/
    }

    private static void handleBatch(List<I18nText> texts, ZhipuBatchKeyGenerator batchGenerator, Map<String, String> output, int batchSize)
        throws InterruptedException, IOException {

        List<List<I18nText>> partitions = Lists.partition(texts, batchSize);

        for (List<I18nText> batch : partitions) {

            // 1️⃣ 提取本批中文（保持顺序）
            List<String> chineseBatch = batch.stream()
                .map(I18nText::getChinese)
                .collect(Collectors.toList());

            // 2️⃣ 批量翻译
            List<String> keys = batchGenerator.generateBatch(chineseBatch);

            System.out.println("keys: " + keys.size() + " == chinese_size: " + chineseBatch.size());

            if (keys.size() != chineseBatch.size()) {
                System.out.println("==============存在行数不一致，进入细分批次处理============");
                batchSize = batchSize / 2;
                handleBatch(batch, batchGenerator, output, batchSize);
                batchSize = BATCH_SIZE;
                continue;
            }

            // 3️⃣ 一一对应写入
            int index = 2;
            for (int i = 0; i < batch.size(); i++) {
                I18nText text = batch.get(i);
                String key = keys.get(i);

                String fullKey = text.getOwnerFqn() + "." + key;

                if (output.containsKey(fullKey)) {
                    fullKey = fullKey + "." + index;
                    index++;
                }
                output.put(fullKey, text.getChinese());

                System.out.println(fullKey + "=" + text.getChinese());
            }
        }
        PropertiesWrite.writeProperties(OUTPUT, output);
    }



}
