package i18n.utils;

import static i18n.CommonContext.ENUM_FILE_EXIST;
import static i18n.CommonContext.OUTPUT;
import static i18n.CommonContext.PROJECT_ROOT;
import static i18n.CommonContext.SIMPLE_MODE;
import static i18n.app.I18nScanApplication.classScan;
import static i18n.app.I18nScanApplication.enumScan;
import static i18n.app.I18nSourceReplacer.isTestPath;

import i18n.app.I18nSourceReplacer;
import i18n.replace.I18nIndex;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public class I18nScanner {

    public static void main(String[] args) throws Exception {
        scan(PROJECT_ROOT , true , false);
//        String replacePath = "D:\\workspace_new\\dmall-fit-xiaoshou-pifa\\dmall-fit-xiaoshou-pifa-os\\dmall-fit-xiaoshou-pifa-os-application\\src\\main\\java\\com\\dmall\\fit\\xiaoshou\\pifa\\os\\app\\dto\\jiaoyi\\request\\salesorder";
//        replace(replacePath);
    }
    /**
     * i18n国际化文案扫描 默认重新全量扫描枚举和class
     * @param projectRoot 项目路径
     *    simpleMode 默认为 true
     *    simpleMode = true 简单模式 例如：SalesOrderExportDto.i18n_w41d3p2u=备注
     *    simpleMode = false AI翻译模式 例如：SalesOrderExportDto.sales_order_amount=销售单金额
     *    AI翻译模式会调用智普AI,会比较慢 如果失败，则自行删除文件里的键值对后重试。
     */
    public static void scan(String projectRoot) throws Exception {
        scan(projectRoot, ENUM_FILE_EXIST, SIMPLE_MODE);
    }

    /**
     * i18n国际化文案扫描
     * @param projectRoot 项目路径
     * @param enumFileExist 枚举是否扫描完毕
     * @param simpleMode 是否采用简单模式
     *    simpleMode = true 简单模式 例如：SalesOrderExportDto.i18n_w41d3p2u=备注
     *    simpleMode = false AI翻译模式 例如：SalesOrderExportDto.sales_order_amount=销售单金额
     *    AI翻译模式会调用智普AI,会比较慢 如果失败，则自行删除文件里的键值对后重试。
     */
    public static void scan(String projectRoot, boolean enumFileExist, boolean simpleMode) throws Exception {
        Path root = Paths.get(projectRoot);
        Map<String, String> output = new LinkedHashMap<>();
        if (!enumFileExist) {
            enumScan(root, output);
        }
        Map<String, String> output1 = new LinkedHashMap<>();
        classScan(root, output1, simpleMode);
    }

    /**
     * i18n国际化回填
     * @param replacePath 需要回填的包路径
     */
    public static void replace(String replacePath) throws Exception {
        String properties = "src/main/resources/i18n/messages_zh_CN.properties";
        Path out = Paths.get(properties);
        I18nIndex index = I18nIndex.load(out);
        I18nSourceReplacer replacer = new I18nSourceReplacer(index);

        Path projectRoot = Paths.get(replacePath);
        try (Stream<Path> paths = Files.walk(projectRoot)) {

            paths.filter(p -> p.toString().endsWith(".java")).filter(p -> !isTestPath(p)).forEach(p -> {
                try {
                    replacer.replace(p);
                } catch (Exception e) {
                    throw new RuntimeException("Replace failed: " + p, e);
                }
            });
        }
    }


}
