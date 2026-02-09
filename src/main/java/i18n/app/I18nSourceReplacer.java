package i18n.app;

import static i18n.CommonContext.OUTPUT;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import i18n.replace.EnumI18nVisitor;
import i18n.replace.I18nIndex;
import i18n.replace.I18nReplaceVisitor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * 源码国际化文案替换器
 * @author yao.xiao
 */
public class I18nSourceReplacer {

    private final I18nIndex index;

    public I18nSourceReplacer(I18nIndex index) {
        this.index = index;
    }

    public void replace(Path javaFile) throws Exception {

        CompilationUnit cu = StaticJavaParser.parse(javaFile);


        // ⭐ 必须启用 LP
        LexicalPreservingPrinter.setup(cu);

        //  枚举优先
        cu.accept(new EnumI18nVisitor(index), null);

        I18nReplaceVisitor visitor =
            new I18nReplaceVisitor(index);

        for (ClassOrInterfaceDeclaration cls :
            cu.findAll(ClassOrInterfaceDeclaration.class)) {

            // ⭐ classFqn
            String classFqn = cls.getNameAsString();

            visitor.visit(cls, classFqn);
        }

        // ⭐ 只能用 LP 输出
        Files.writeString(
            javaFile,
            LexicalPreservingPrinter.print(cu)
        );
    }

    public static void main(String[] args) throws Exception {

        String javaPath =
            "D:\\workspace_new\\dmall-fit-xiaoshou-pifa\\dmall-fit-xiaoshou-pifa-foundation\\dmall-fit-xiaoshou-pifa-spec\\src\\main\\java\\com\\dmall\\fit\\xiaoshou\\pifa\\foundation\\spec\\enums";

        Path out = Paths.get(OUTPUT);
        Path projectRoot = Paths.get(javaPath);

        I18nIndex index = I18nIndex.load(out);
        I18nSourceReplacer replacer = new I18nSourceReplacer(index);

        try (Stream<Path> paths = Files.walk(projectRoot)) {

            paths.filter(p -> p.toString().endsWith(".java"))
                .filter(p -> !isTestPath(p))
                .forEach(p -> {
                    try {
                        replacer.replace(p);
                    } catch (Exception e) {
                        throw new RuntimeException("Replace failed: " + p, e);
                    }
                });
        }
    }

    public static boolean isTestPath(Path path) {
        for (Path part : path) {
            if ("test".equalsIgnoreCase(part.toString())) {
                return true;
            }
        }
        return false;
    }
}
