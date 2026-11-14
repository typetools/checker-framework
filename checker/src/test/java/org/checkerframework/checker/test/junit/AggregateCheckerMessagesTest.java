package org.checkerframework.checker.test.junit;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that a checker prints the message text of a key that only its subchecker defines.
 * Per-directory tests do not notice missing text, because the test framework compares an expected
 * diagnostic with an issued one by its message key, not by its message text.
 */
public class AggregateCheckerMessagesTest {

  /** The directory that holds the source and class files. */
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  /**
   * Code for which the Resource Leak Checker reports {@code required.method.not.called} and {@code
   * missing.creates.mustcall.for}, whose text {@code rlccalledmethods/messages.properties} defines.
   * The field is named {@code bound}, which is also a message key in {@code
   * basetype/messages.properties}.
   */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "import org.checkerframework.checker.mustcall.qual.InheritableMustCall;",
          "import org.checkerframework.checker.mustcall.qual.Owning;",
          "@InheritableMustCall(\"close\") class Resource {",
          "  void close() {}",
          "}",
          "class Leak {",
          "  @Owning Resource bound;",
          "  void leak() {",
          "    Resource r = new Resource();",
          "  }",
          "  void reassign() {",
          "    bound = new Resource();",
          "  }",
          "}");

  /**
   * Tests that the Resource Leak Checker prints the text that its subchecker defines, and that it
   * does not replace a field name that is also a subchecker's message key.
   *
   * @throws IOException if a file cannot be read or written
   */
  @Test
  public void resourceLeakCheckerPrintsSubcheckerMessageText() throws IOException {
    List<String> messages = compile();
    assertSomeMessageContains(
        messages,
        "[required.method.not.called] @MustCall method close may not have been invoked on r or");
    assertSomeMessageContains(
        messages,
        "[missing.creates.mustcall.for] Method reassign re-assigns the non-final, owning field"
            + " this.bound,");
  }

  /**
   * Asserts that one of the given messages contains the given text.
   *
   * @param messages diagnostic messages
   * @param expected the text that one of {@code messages} should contain
   */
  private static void assertSomeMessageContains(List<String> messages, String expected) {
    for (String message : messages) {
      if (message.contains(expected)) {
        return;
      }
    }
    Assert.fail("No message contains \"" + expected + "\".  Messages: " + messages);
  }

  /**
   * Compiles {@link #SOURCE} with the Resource Leak Checker.
   *
   * @return the messages of the diagnostics that the compilation issues
   * @throws IOException if a file cannot be read or written
   */
  private List<String> compile() throws IOException {
    File sourceFile = tempFolder.newFolder("src").toPath().resolve("Leak.java").toFile();
    Files.write(sourceFile.toPath(), SOURCE.getBytes(StandardCharsets.UTF_8));
    File classesDir = tempFolder.newFolder("classes");

    List<String> options =
        Arrays.asList(
            "-processor",
            ResourceLeakChecker.class.getCanonicalName(),
            "-ApermitMissingJdk",
            "-classpath",
            System.getProperty("java.class.path"),
            "-d",
            classesDir.toString());

    StringWriter javacOutput = new StringWriter();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles =
          fileManager.getJavaFileObjects(new File[] {sourceFile});
      // The compilation is expected to fail, because the checker issues errors.
      compiler
          .getTask(
              javacOutput, fileManager, diagnostics, options, Collections.emptyList(), javaFiles)
          .call();
    }

    List<String> result = new ArrayList<>();
    for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
      result.add(diagnostic.getMessage(Locale.ROOT));
    }
    return result;
  }
}
