package i18n.zai;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ZhipuBatchKeyGenerator {

    private final ZhipuAiClient client;

    public ZhipuBatchKeyGenerator(String apiKey) {
        this.client = ZhipuAiClient.builder()
            .ofZHIPU()
            .apiKey(apiKey)
            .build();
    }

    public List<String> generateBatch(List<String> chineseList) throws InterruptedException{
        String prompt = buildPrompt(chineseList);

        String result = ZAiUnit.zaiTranslate(prompt, client);

        return Arrays.stream(result.split("\n"))
            .map(String::trim)
            .map(this::normalize)
            .collect(Collectors.toList());
    }

    private String buildPrompt(List<String> list) {
        String prompt = "你是一个翻译程序，用于将中文文案转换为 简短、snake_case 格式的英文 key。\n"
            + "规则（必须严格遵守）：\n"

            + "输入是一个有序的中文列表，每一行都是一个独立的中文文案\n"

            + "输出必须逐行对应输入：\n"

            + "输出行数 必须与输入行数完全一致\n"

            + "严禁出现以下情况：\n"

            + "合并、拆分、重排中文\n"

            + "对中文进行解释或补充说明\n"

            + "输出内容要求：\n"

            + "仅输出英文 key\n"

            + "使用小写字母 + 下划线（snake_case）\n"

            + "含义贴近原中文，但保持简短、稳定\n"

            + "输出的 key 前缀不要拼接行号\n"

            + "除结果本身外，不要输出任何多余内容。\n "

            + "中文列表如下： \n";

        StringBuilder sb = new StringBuilder(prompt);

        for (int i = 0; i < list.size(); i++) {
            sb.append(i + 1).append(".").append(list.get(i)).append("\n");
        }
        return sb.toString();
    }

    private String normalize(String s) {
        return s.toLowerCase()
            .replaceAll("[^a-z0-9_]", "")
            .replaceAll("_+", "_");
    }
}
