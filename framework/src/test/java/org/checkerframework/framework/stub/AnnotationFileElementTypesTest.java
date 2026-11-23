package org.checkerframework.framework.stub;

import static org.checkerframework.framework.stub.AnnotationFileTestUtil.write;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
 * Tests the diagnostics that {@link AnnotationFileElementTypes} issues about the annotation files
 * named by the {@code -Astubs} command-line option.
 */
public class AnnotationFileElementTypesTest {

  /** A temporary directory, deleted after each test. */
  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  /**
   * Compiles a trivial class with the Value Checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs) throws IOException {
    return diagnostics(stubs, "org.checkerframework.common.value.ValueChecker");
  }

  /**
   * Compiles a trivial class with the given checker and the given {@code -Astubs} command-line
   * option, and returns the compiler's diagnostics.
   *
   * @param stubs the argument to the {@code -Astubs} command-line option
   * @param checker the fully-qualified name of a checker, such as
   *     "org.checkerframework.common.value.ValueChecker"
   * @return the diagnostics, each formatted as its kind, a colon, and its message, such as
   *     "WARNING: Ignoring Foo.stub: ..."
   * @throws IOException if the source file cannot be written or the compiler cannot be run
   */
  private List<String> diagnostics(String stubs, String checker) throws IOException {
    File sourceFile = write(new File(tmp.getRoot(), "src/Test.java"), "class Test {}");
    File classes = tmp.newFolder();
    List<String> options =
        Arrays.asList(
            "-processor",
            checker,
            "-Astubs=" + stubs,
            "-ApermitMissingJdk",
            "-d",
            classes.getPath(),
            "-classpath",
            System.getProperty("java.class.path"));
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles = fileManager.getJavaFileObjects(sourceFile);
      compiler.getTask(null, fileManager, collector, options, null, javaFiles).call();
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
    List<String> result = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
      result.add(diagnostic.getKind() + ": " + diagnostic.getMessage(Locale.ROOT));
    }
    return result;
  }

  /**
   * Asserts that exactly one of the given diagnostics contains the given text, and that that
   * diagnostic is a warning.
   *
   * @param text the expected text of a diagnostic, such as "Could not read annotation resource"
   * @param diagnostics the diagnostics that the compiler issued
   */
  private static void assertOneWarning(String text, List<String> diagnostics) {
    List<String> matching = new ArrayList<>();
    for (String diagnostic : diagnostics) {
      if (diagnostic.contains(text)) {
        matching.add(diagnostic);
      }
    }
    Assert.assertEquals(
        "Diagnostics that contain \"" + text + "\"; all diagnostics: " + diagnostics,
        1,
        matching.size());
    Assert.assertTrue(matching.get(0), matching.get(0).startsWith("WARNING: "));
  }

  /** Tests that a named archive that cannot be opened yields one warning. */
  @Test
  public void corruptArchive() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");
    assertOneWarning(
        "Could not read annotation resource: " + jar.getPath() + ": ", diagnostics(jar.getPath()));
  }

  /**
   * Tests that a compound checker, each of whose checkers reads the named archive, yields one
   * warning about an archive that cannot be opened.
   */
  @Test
  public void corruptArchiveCompoundChecker() throws IOException {
    File jar = write(new File(tmp.getRoot(), "stubs.jar"), "not a jar file");
    assertOneWarning(
        "Could not read annotation resource: " + jar.getPath() + ": ",
        diagnostics(
            jar.getPath(), "org.checkerframework.framework.testchecker.compound.CompoundChecker"));
  }
}
