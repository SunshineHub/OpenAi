package i18n.zai;


import java.util.List;

public class TranslatePromptBuilder {

    public static String build(List<String> source, String targetLang) {
        StringBuilder sb = new StringBuilder();

        sb.append("你是一个专业的国际化翻译引擎。\n")
            .append("请将下面每一行中文翻译成【")
            .append(targetLang)
            .append("】。\n")
            .append("规则：\n")
            .append("1. 必须严格保持顺序\n")
            .append("2. 必须一一对应，不可遗漏、不可新增\n")
            .append("3. 每行只输出翻译结果，不要解释\n")
            .append("4. 保留占位符（如 %s、{0}）不变\n")
            .append("\n中文列表：\n");

        for (int i = 0; i < source.size(); i++) {
            sb.append(i + 1).append(". ")
                .append(source.get(i)).append("\n");
        }
        return sb.toString();
    }
}

