package i18n.replace;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import i18n.scan.I18nFilter;
import java.util.Optional;

public class I18nReplaceVisitor extends ModifierVisitor<String> {

    private final I18nIndex index;

    public I18nReplaceVisitor(I18nIndex index) {
        this.index = index;
    }

    @Override
    public Visitable visit(StringLiteralExpr n, String classFqn) {

        // ⭐ enum 内跳过
        if (isInsideEnum(n)) {
            return n;
        }

        String value = n.getValue();

        if (!ChineseUtil.containsChinese(value)) {
            return n;
        }

        Optional<AnnotationExpr> annOpt = n.findAncestor(AnnotationExpr.class);

        // ⭐ 在注解内
        if (annOpt.isPresent()) {

            AnnotationExpr ann = annOpt.get();

            // Swagger 不动
            if (I18nFilter.isSwaggerAnnotation(ann)) {
                return n;
            }

            Optional<String> keyOpt = index.findKey(classFqn, value);
            if (!keyOpt.isPresent()) {
                return n;
            }

            String key = keyOpt.get();

            // ⭐ Bean Validation 注解 → {key}
            if (I18nFilter.isValidationAnnotation(ann)) {
                return new StringLiteralExpr("{" + key + "}");
            }

            // ⭐ 普通注解 → "key"
            return new StringLiteralExpr(key);
        }

        // ⭐ 普通代码 → I18nUtils.get
        Optional<String> keyOpt = index.findKey(classFqn, value);

        if (keyOpt.isPresent()) {
            return StaticJavaParser.parseExpression(
                "I18nUtils.get(\"" + keyOpt.get() + "\")"
            );
        }

        return n;
    }

    /* ================= helper ================= */

    private boolean isInsideEnum(StringLiteralExpr n) {
        return n.findAncestor(EnumDeclaration.class).isPresent();
    }


}
