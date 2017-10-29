// Note that "-processor org/checkerframework/checker.nullness.NullnessChecker"
// is added to the invocation of the compiler!
// TODO: add a @Processor method-annotation to parameterize

/**
 * This class has auxiliar methods to compile a class and return its classfile. It is used by
 * defaultPersists/Driver and inheritDeclAnnoPersist/Driver.
 */
import com.sun.tools.classfile.ClassFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

public class PersistUtil {

    public static String testClassOf(Method m) {
        TestClass tc = m.getAnnotation(TestClass.class);
        if (tc != null) {
            return tc.value();
        } else {
            return "Test";
        }
    }

    public static ClassFile compileAndReturn(String fullFile, String testClass) throws Exception {
        File source = writeTestFile(fullFile);
        File clazzFile = compileTestFile(source, testClass);
        return ClassFile.read(clazzFile);
    }

    public static File writeTestFile(String fullFile) throws IOException {
        File f = new File("Test.java");
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(f)));
        out.println(fullFile);
        out.close();
        return f;
    }

    public static File compileTestFile(File f, String testClass) {
        int rc =
                com.sun.tools.javac.Main.compile(
                        new String[] {
                            "-source",
                            "1.8",
                            "-g",
                            "-processor",
                            "org.checkerframework.checker.nullness.NullnessChecker",
                            f.getPath()
                        });
        if (rc != 0) {
            throw new Error("compilation failed. rc=" + rc);
        }
        String path;
        if (f.getParent() != null) {
            path = f.getParent();
        } else {
            path = "";
        }

        return new File(path + testClass + ".class");
    }

    public static String wrap(String compact) {
        StringBuilder sb = new StringBuilder();

        // Automatically import java.util
        sb.append("\nimport java.util.*;");
        sb.append("\nimport java.lang.annotation.*;\n");

        // And the Nullness qualifiers
        sb.append("import org.checkerframework.framework.qual.DefaultQualifier;\n");
        sb.append("import org.checkerframework.checker.nullness.qual.*;\n");
        sb.append("import org.checkerframework.dataflow.qual.*;\n");

        sb.append("\n");
        boolean isSnippet =
                !(compact.startsWith("class") || compact.contains(" class"))
                        && !compact.contains("interface")
                        && !compact.contains("enum");

        if (isSnippet) {
            sb.append("class Test {\n");
        }

        sb.append(compact);
        sb.append("\n");

        if (isSnippet) {
            sb.append("}\n\n");
        }

        return sb.toString();
    }
}

/**
 * The name of the class that should be analyzed. Should only need to be provided when analyzing
 * inner classes.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface TestClass {
    String value() default "Test";
}
