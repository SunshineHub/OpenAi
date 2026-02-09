package i18n.replace;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import i18n.scan.I18nFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EnumI18nVisitor extends VoidVisitorAdapter<String> {

    private final I18nIndex index;

    public EnumI18nVisitor(I18nIndex index) {
        this.index = index;
    }

    @Override
    public void visit(EnumDeclaration enumDecl, String arg) {

        String enumFqn = enumDecl.getNameAsString();

        // 所有非 static 字段
        List<FieldDeclaration> fields = new ArrayList<>();
        for (FieldDeclaration field : enumDecl.getFields()) {
            if (!field.isStatic()) fields.add(field);
        }

        if (fields.isEmpty()) {
            super.visit(enumDecl, arg);
            return;
        }

        removeLombokAnnotations(enumDecl);

        // 枚举常量构造参数对应字段
        enumDecl.getEntries().forEach(entry -> {
            List<StringLiteralExpr> literals = new ArrayList<>();
            entry.getArguments().forEach(argExpr -> {
                if (argExpr instanceof StringLiteralExpr) literals.add((StringLiteralExpr) argExpr);
            });

            int stringIndex = 0;
            for (FieldDeclaration fieldDecl : fields) {
                for (VariableDeclarator var : fieldDecl.getVariables()) {
                    String fieldName = var.getNameAsString();
                    String fieldType = var.getType().asString();

                    Optional<String> key = Optional.empty();

                    // 对应字符串文案的枚举构造参数
                    if (fieldType.equals("String") && stringIndex < literals.size()) {
                        StringLiteralExpr literal = literals.get(stringIndex);
                        if (I18nFilter.accept(literal)) {
                            key = index.findKey(enumFqn, literal.getValue());
                        }
                        stringIndex++;
                    }

                    ensureGetter(enumDecl, fieldName, fieldType, key);
                    ensureSetter(enumDecl, fieldName, fieldType);
                }
            }
        });

        super.visit(enumDecl, arg);
    }

    private void removeLombokAnnotations(EnumDeclaration enumDecl) {
        enumDecl.getAnnotations().removeIf(ann -> {
            String name = ann.getNameAsString();
            return name.equals("Data") || name.equals("Getter") || name.equals("Setter");
        });
    }

    private void ensureGetter(EnumDeclaration enumDecl, String fieldName, String fieldType, Optional<String> key) {
        String methodName = "get" + capitalize(fieldName);

        // 删除旧 getter
        enumDecl.getMethodsByName(methodName).forEach(MethodDeclaration::remove);

        MethodDeclaration getter = enumDecl.addMethod(methodName, Modifier.Keyword.PUBLIC);
        getter.setType(fieldType);

        if (key.isPresent()) {
            String block = "{\n" +
                "    String val = I18nUtils.get(\"" + enumDecl.getNameAsString() + ".\" + this.name() + \"."
                + fieldName + "\");\n" +
                "    if (val == null || val.trim().isEmpty()) {\n" +
                "        return this." + fieldName + ";\n" +
                "    }\n" +
                "    return val;\n" +
                "}";
            getter.setBody(StaticJavaParser.parseBlock(block));
        } else {
            getter.setBody(StaticJavaParser.parseBlock("{ return this." + fieldName + "; }"));
        }
    }

    private void ensureSetter(EnumDeclaration enumDecl, String fieldName, String fieldType) {
        String methodName = "set" + capitalize(fieldName);
        if (!enumDecl.getMethodsByName(methodName).isEmpty()) return;

        MethodDeclaration setter = enumDecl.addMethod(methodName, Modifier.Keyword.PUBLIC);
        setter.setType("void");
        setter.addParameter(fieldType, fieldName);
        setter.setBody(StaticJavaParser.parseBlock("{ this." + fieldName + " = " + fieldName + "; }"));
    }

    private String capitalize(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
