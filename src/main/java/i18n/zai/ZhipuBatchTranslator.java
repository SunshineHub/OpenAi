package i18n.zai;


import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;

import ai.z.openapi.service.model.ChatThinking;
import java.util.ArrayList;
import java.util.List;

public class ZhipuBatchTranslator {

    private final ZhipuAiClient client;
    private final String targetLang;

    public ZhipuBatchTranslator(String apiKey, String targetLang) {
        this.client = ZhipuAiClient.builder()
            .ofZHIPU()
            .apiKey(apiKey)
            .build();
        this.targetLang = targetLang;
    }

    public List<String> translate(List<String> source) {


        String prompt =
            TranslatePromptBuilder.build(source, targetLang);

        String result = ZAiUnit.zaiTranslate(prompt, client);

        return parseLines(result, source.size());
    }

    private List<String> parseLines(String text, int size) {
        String[] lines = text.split("\\r?\\n");
        List<String> result = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            int idx = line.indexOf('.');
            if (idx > 0) {
                line = line.substring(idx + 1).trim();
            }
            result.add(line);
        }
        return result;
    }
}

