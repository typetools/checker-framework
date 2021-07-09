// Note that "-processor org/checkerframework/checker.nullness.NullnessChecker"
// is added to the invocation of the compiler!
// TODO: add a @Processor method-annotation to parameterize

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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.StringJoiner;

/**
 * This class has auxiliary methods to compile a class and return its classfile. It is used by
 * defaultPersists/Driver and inheritDeclAnnoPersist/Driver.
 */
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

    File result = new File(path + testClass + ".class");

    // This diagnostic code preserves temporary files and prints the paths where they are preserved.
    if (false) {
      try {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        File fCopy = File.createTempFile("FCopy", ".java", tempDir);
        File resultCopy = File.createTempFile("FCopy", ".class", tempDir);
        // REPLACE_EXISTING is essential in the `Files.copy()` calls because createTempFile actually
        // creates a file in addition to returning its name.
        Files.copy(f.toPath(), fCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(result.toPath(), resultCopy.toPath(), StandardCopyOption.REPLACE_EXISTING);
        System.out.printf("comileTestFile: copied to %s %s%n", fCopy, resultCopy);
      } catch (IOException e) {
        throw new Error(e);
      }
    }

    return result;
  }

  public static String wrap(String compact) {
    StringJoiner sj = new StringJoiner(System.lineSeparator());

    // Automatically import java.util
    sj.add("");
    sj.add("import java.util.*;");
    sj.add("import java.lang.annotation.*;");

    // And the Nullness qualifiers
    sj.add("import org.checkerframework.framework.qual.DefaultQualifier;");
    sj.add("import org.checkerframework.checker.nullness.qual.*;");
    sj.add("import org.checkerframework.dataflow.qual.*;");

    sj.add("");
    boolean isSnippet =
        !(compact.startsWith("class") || compact.contains(" class"))
            && !compact.contains("interface")
            && !compact.contains("enum");

    if (isSnippet) {
      sj.add("class Test {");
    }

    sj.add(compact);

    if (isSnippet) {
      sj.add("}");
      sj.add("");
    }

    return sj.toString();
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
