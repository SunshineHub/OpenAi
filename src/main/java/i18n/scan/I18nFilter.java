package i18n.scan;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import java.util.regex.Pattern;

public class I18nFilter {
    private static final Pattern LOG_METHOD_PATTERN =
        Pattern.compile("^(trace|debug|info|warn|error|fatal|print|println)$",
            Pattern.CASE_INSENSITIVE);
    private static final String SWAGGER_PACKAGE_PREFIX = "io.swagger.annotations";

    private static final Pattern SQL_PATTERN =
        Pattern.compile(
            "(?i)^\\s*(select|insert|update|delete)\\s+.+\\s+(from|into|set)\\s+.+"
        );


    private static final String[] VALIDATION_PACKAGES = {
        "javax.validation",
        "jakarta.validation",
        "org.hibernate.validator"
    };

    private static final String[] VALIDATION_SIMPLE_NAMES = {
        "NotNull",
        "NotBlank",
        "NotEmpty",
        "Size",
        "Min",
        "Max",
        "Pattern",
        "Email",
        "Length",
        "Range"
    };

    public static boolean accept(StringLiteralExpr expr) {

        String v = expr.getValue();

        if (!containsChinese(v)) return false;
        if (isLog(expr)) return false;
        if (isSwaggerAnnotation(expr)) return false;
        if (isConfig(expr)) return false;
        if (isSqlOrTech(v)) return false;
        if (isI18n(expr)) return false;

        return true;
    }


    private static boolean containsChinese(String s) {
        return s.matches(".*[\u4e00-\u9fa5].*");
    }


    private static boolean isLog(Node node) {

        return node.findAncestor(MethodCallExpr.class)
            .map(call -> {

                // 方法名：info / warn / error / println ...
                String method = call.getNameAsString();

                if (!LOG_METHOD_PATTERN.matcher(method).matches()) {
                    return false;
                }

                // scope：log / logger / LOGGER / System.out
                return call.getScope()
                    .map(s -> {
                        String scope = s.toString().toLowerCase();
                        return scope.contains("log")
                            || scope.contains("logger")
                            || scope.contains("system.out")
                            || scope.contains("system.err");
                    })
                    .orElse(false);

            })
            .orElse(false);
    }



    private static boolean isConfig(Node node) {
        return node.findAncestor(AnnotationExpr.class)
            .map(a -> a.toString().contains("@Value"))
            .orElse(false);
    }


    private static boolean isI18n(Node node) {
        return node.toString().contains("I18n.get")
            || node.toString().contains("getMessage(");
    }


    private static boolean isSqlOrTech(String v) {

        String s = v.trim();

        // SQL（强约束）
        if (SQL_PATTERN.matcher(s).matches()) {
            return true;
        }

        // 纯占位符
        if (isPureTemplate(s)) {
            return true;
        }

        // 纯 URL
        if (isPureUrl(s)) {
            return true;
        }

        // JSON / XML 模板
        if (looksLikeJsonOrXml(s)) {
            return true;
        }

        return false;
    }

    private static boolean isPureTemplate(String v) {
        return v.matches("^\\{[^}]+}$");
    }

    private static boolean isPureUrl(String v) {
        return v.matches("^(https?|ftp)://.+");
    }

    private static boolean looksLikeJsonOrXml(String v) {
        String s = v.trim();
        return (s.startsWith("{") && s.endsWith("}"))
            || (s.startsWith("[") && s.endsWith("]"))
            || (s.startsWith("<") && s.endsWith(">"));
    }


    public static boolean isSwaggerAnnotation(Node node) {

        return node.findAncestor(AnnotationExpr.class)
            .map(anno -> {

                // 注解名（ApiOperation / ApiModelProperty）
                String name = anno.getNameAsString();

                // 快速兜底（常见 swagger 注解名）
                if (name.startsWith("Api")) {
                    return true;
                }

                // 更严格：尝试通过 import 判断包名
                return anno.findCompilationUnit()
                    .flatMap(cu -> cu.getImports().stream()
                        .filter(i -> !i.isAsterisk())
                        .filter(i -> i.getNameAsString().endsWith(name))
                        .findFirst()
                        .map(i -> i.getNameAsString()
                            .startsWith(SWAGGER_PACKAGE_PREFIX)))
                    .orElse(false);
            })
            .orElse(false);
    }

    public static boolean isValidationAnnotation(AnnotationExpr ann) {

        String name = ann.getNameAsString();

        // ⭐ 常见 validation 注解名兜底
        if (name.equals("NotBlank")
            || name.equals("NotNull")
            || name.equals("NotEmpty")
            || name.equals("Size")
            || name.equals("Length")
            || name.equals("Pattern")
            || name.equals("Email")
            || name.equals("Min")
            || name.equals("Max")
            || name.equals("Range")
            || name.equals("Positive")
            || name.equals("PositiveOrZero")
            || name.equals("Negative")
            || name.equals("NegativeOrZero")
            || name.equals("AssertTrue")
            || name.equals("AssertFalse")) {

            return true;
        }

        // ⭐ 通过 import 判断包名（最稳）
        return ann.findCompilationUnit()
            .flatMap(cu -> cu.getImports().stream()
                .filter(i -> !i.isAsterisk())
                .filter(i -> i.getNameAsString().endsWith("." + name))
                .findFirst()
                .map(i -> {
                    String pkg = i.getNameAsString();

                    return pkg.startsWith("javax.validation")
                        || pkg.startsWith("jakarta.validation")
                        || pkg.startsWith("org.hibernate.validator");
                })
            )
            .orElse(false);
    }



}
