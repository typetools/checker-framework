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
        StringBuilder sb = new StringBuilder(System.lineSeparator());

        // Automatically import java.util
        sb.append("");
        sb.append("import java.util.*;");
        sb.append("import java.lang.annotation.*;");

        // And the Nullness qualifiers
        sb.append("import org.checkerframework.framework.qual.DefaultQualifier;");
        sb.append("import org.checkerframework.checker.nullness.qual.*;");
        sb.append("import org.checkerframework.dataflow.qual.*;");

        sb.append("");
        boolean isSnippet =
                !(compact.startsWith("class") || compact.contains(" class"))
                        && !compact.contains("interface")
                        && !compact.contains("enum");

        if (isSnippet) {
            sb.append("class Test {");
        }

        sb.append(compact);

        if (isSnippet) {
            sb.append("}");
            sb.append("");
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
