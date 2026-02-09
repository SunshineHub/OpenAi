package i18n.zai;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.model.ChatCompletionCreateParams;
import ai.z.openapi.service.model.ChatCompletionResponse;
import ai.z.openapi.service.model.ChatMessage;
import ai.z.openapi.service.model.ChatMessageRole;
import ai.z.openapi.service.model.ChatThinking;
import java.util.List;

public class ZAiUnit {

    public static String zaiTranslate(String prompt , ZhipuAiClient client) {
        ChatCompletionCreateParams request =
            ChatCompletionCreateParams.builder()
                .model("glm-4.7")
                .messages(List.of(
                    ChatMessage.builder()
                        .role(ChatMessageRole.USER.value())
                        .content(prompt)
                        .build()
                ))
                .thinking(ChatThinking.builder().type("disabled").build())
//                .stream(false)  // 启用流式输出
                .maxTokens(65536)
                .temperature(0.8f)
                .doSample(false)
                .build();

        // 发送请求
        ChatCompletionResponse response = client.chat().createChatCompletion(request);

        if (!response.isSuccess()) {
            throw new RuntimeException(response.getError().getMessage());
        }

        StringBuilder result = new StringBuilder();
        // 获取回复
        if (response.getData().getChoices() == null || response.getData().getChoices().isEmpty()) {
            result.append("\n");
        }
        ChatMessage delta = response.getData().getChoices().get(0).getMessage();
        if (delta != null && delta.getContent() != null) {
            result.append(delta.getContent());
        }
        return result.toString();
    }

}
