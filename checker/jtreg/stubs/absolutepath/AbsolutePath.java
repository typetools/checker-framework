/*
 * @test
 * @summary Test that an absolute path in -Astubs is not resolved against test.src.
 *
 * @compile AbsolutePath.java
 * @run main AbsolutePath
 */

import java.io.File;
import java.util.Arrays;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.checker.nullness.NullnessChecker;

/**
 * Runs the Nullness Checker with an absolute {@code -Astubs} path. jtreg sets the {@code test.src}
 * system property, which the Checker Framework uses to resolve relative annotation-file paths.
 */
public class AbsolutePath {

  public static void main(String[] args) throws Exception {
    String testSrc = System.getProperty("test.src");
    if (testSrc == null) {
      throw new Error("test.src is not set");
    }
    File stubFile = new File(testSrc, "NonNullGetProperty.astub").getAbsoluteFile();
    File testFile = new File(testSrc, "UseStub.java");

    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = javac.getStandardFileManager(null, null, null)) {
      JavaCompiler.CompilationTask task =
          javac.getTask(
              null,
              fileManager,
              null,
              Arrays.asList(
                  "-classpath",
                  System.getProperty("java.class.path"),
                  "-proc:only",
                  "-Werror",
                  "-Astubs=" + stubFile.getPath()),
              null,
              fileManager.getJavaFileObjects(testFile));
      task.setProcessors(Arrays.asList(new NullnessChecker()));
      if (!task.call()) {
        throw new Error("Type-checking with -Astubs=" + stubFile + " failed");
      }
    }
  }
}
