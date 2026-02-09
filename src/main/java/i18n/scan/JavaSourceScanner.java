package i18n.scan;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import i18n.zai.I18nText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class JavaSourceScanner {


    public List<I18nText> scan(Path root) throws IOException {
        List<I18nText> result = new ArrayList<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isTestPath(p))
                .forEach(p -> parseFile(p, result));
        }
        return result;
    }

    public List<I18nText> scanEnums(Path root) throws IOException {
        List<I18nText> result = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(root)) {
            paths
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isTestPath(p))
                .forEach(p -> parseEnumFile(p, result));
        }
        return result;
    }

    private boolean isTestPath(Path path) {
        for (Path part : path) {
            if ("test".equalsIgnoreCase(part.toString())) {
                return true;
            }
        }
        return false;
    }


    private void parseFile(Path file, List<I18nText> out) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            String pkg = cu.getPackageDeclaration()
                .map(NodeWithName::getNameAsString)
                .orElse("");

            // ⭐ 同一文件内的文案去重集合
            // key = 处理后的 value
            // value 不关心 class，只关心文案本身
            java.util.Set<String> seenInFile = new java.util.HashSet<>();

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(clazz -> {

                /*String classFqn = pkg.isEmpty()
                    ? clazz.getNameAsString()
                    : pkg + "." + clazz.getNameAsString();*/
                String classFqn = clazz.getNameAsString();

                clazz.findAll(StringLiteralExpr.class).forEach(literal -> {
                    String value = literal.getValue();

                    // 统一处理换行
                    if (value.contains("\\n")) {
                        value = value.replace("\\n", "%H");
                    }

                    // 过滤非国际化文案
                    if (!I18nFilter.accept(literal)) {
                        return;
                    }

                    // ⭐ 同文件内已出现过，直接跳过
                    if (!seenInFile.add(value)) {
                        return;
                    }

                    out.add(new I18nText(classFqn, value));
                });
            });
        } catch (Exception ignore) {
        }
    }


    private void parseEnumFile(Path file, List<I18nText> out) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            cu.findAll(EnumDeclaration.class).forEach(enumDecl -> {

                String enumFqn = enumDecl.getNameAsString();

                // 构造 String 类型字段列表
                List<String> stringFields = resolveEnumStringFields(enumDecl);

                enumDecl.getEntries().forEach(entry -> {
                    String enumConst = entry.getNameAsString();
                    List<Expression> args = entry.getArguments();

                    int stringIndex = 0;
                    for (Expression arg : args) {
                        if (!(arg instanceof StringLiteralExpr)) {
                            continue;
                        }

                        if (stringIndex >= stringFields.size()) {
                            continue; // 理论上不该发生
                        }

                        StringLiteralExpr literal = (StringLiteralExpr) arg;
                        String value = literal.getValue();

                        if (!I18nFilter.accept(literal)) {
                            stringIndex++;
                            continue;
                        }

                        String field = stringFields.get(stringIndex++);
                        String fullKey = enumFqn + "." + enumConst + "." + field;

                        out.add(new I18nText(fullKey, value));
                    }
                });
            });
        } catch (Exception ignore) {
        }
    }

    public static List<String> resolveEnumStringFields(EnumDeclaration enumDecl) {

        List<String> fields = new ArrayList<>();

        enumDecl.getMembers().forEach(m -> {
            if (!(m instanceof FieldDeclaration)) {
                return;
            }

            FieldDeclaration f = (FieldDeclaration) m;

            // 只关心 String 类型
            if (!f.getElementType().asString().equals("String")) {
                return;
            }

            f.getVariables().forEach(v ->
                fields.add(v.getNameAsString())
            );
        });

        return fields;
    }

}
