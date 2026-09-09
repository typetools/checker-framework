package org.checkerframework.common.wholeprograminference;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests the diagnostics that {@code -AshowWpiFailedInferences} produces when whole-program
 * inference has no storage location for a method.
 */
public class WholeProgramInferenceFailedInferenceTest {

  /**
   * A compilation unit that calls an element of an annotation type. The scenes (JAIF) storage has
   * no storage location for an annotation element, so whole-program inference fails for the call.
   */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package testpkg;",
          "public class UsesAnno {",
          "  String get(MyAnno a) {",
          "    return a.value();",
          "  }",
          "}",
          "@interface MyAnno {",
          "  String value();",
          "}");

  /**
   * Holds the test's source file, class files, and inference output. JUnit deletes it after each
   * test.
   */
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  /**
   * The failure of {@code updateFromMethodInvocation} to find a storage location must be reported,
   * just as the failure of {@code updateFromObjectCreation} is.
   */
  @Test
  public void methodInvocationWithoutStorageLocation() {
    String output = runInference();
    Assert.assertTrue(
        "-AshowWpiFailedInferences did not report the call to MyAnno.value(); output was:"
            + System.lineSeparator()
            + output,
        output.contains(
            "WPI failed to make an inference: WPI could not store information about this method:"
                + " value()Ljava/lang/String;"));
  }

  /**
   * Runs the Value Checker with whole-program inference over {@link #SOURCE} and returns everything
   * that the checker printed to standard output.
   *
   * @return the standard output of the compilation
   */
  private String runInference() {
    Path directory = temporaryFolder.getRoot().toPath();
    Path sourceFile = directory.resolve("UsesAnno.java");
    try {
      Files.write(sourceFile, SOURCE.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    List<String> options =
        Arrays.asList(
            "-processor",
            "org.checkerframework.common.value.ValueChecker",
            "-Ainfer=jaifs",
            "-AinferOutputDirectory=" + directory,
            "-AshowWpiFailedInferences",
            "-Awarns",
            "-ApermitMissingJdk",
            "-d",
            directory.toString(),
            "-classpath",
            System.getProperty("java.class.path"));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
    PrintStream oldOut = System.out;
    System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));
    boolean success;
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles =
          fileManager.getJavaFileObjects(sourceFile.toFile());
      success = compiler.getTask(null, fileManager, diagnostics, options, null, javaFiles).call();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } finally {
      System.out.flush();
      System.setOut(oldOut);
    }
    String output = new String(capturedOutput.toByteArray(), StandardCharsets.UTF_8);
    if (!success) {
      Assert.fail(
          "Compilation of UsesAnno.java failed."
              + System.lineSeparator()
              + "Diagnostics:"
              + System.lineSeparator()
              + diagnosticsToString(diagnostics)
              + "Standard output:"
              + System.lineSeparator()
              + output);
    }
    return output;
  }

  /**
   * Formats compiler diagnostics, one per line, for inclusion in a test failure message.
   *
   * @param diagnostics the diagnostics that the compiler issued
   * @return the diagnostics, one per line
   */
  private static String diagnosticsToString(DiagnosticCollector<JavaFileObject> diagnostics) {
    StringJoiner result = new StringJoiner(System.lineSeparator(), "", System.lineSeparator());
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      result.add(diagnostic.toString());
    }
    return result.toString();
  }
}
