package org.checkerframework.checker.test.junit;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.checkerframework.checker.formatter.FormatterChecker;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that whole-program inference does not infer a {@code @Format} annotation for the format
 * string parameter of a {@code @FormatMethod} method. Such an annotation would be over-restrictive:
 * it would forbid callers from passing a format string of any other conversion category.
 *
 * <p>The format string parameter is the first formal parameter whose type is {@code String}, which
 * is not necessarily the first formal parameter.
 */
public class AinferFormatMethodTest {

  /** The directories that this test writes into. */
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  /** Creates a new AinferFormatMethodTest. */
  public AinferFormatMethodTest() {}

  /**
   * The source code on which inference is run. Method {@code log}'s format string parameter is its
   * second formal parameter. Method {@code notAFormatMethod} is a control: it is not a format
   * method, so inference does annotate its {@code String} parameter.
   */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package wpitest;",
          "import java.util.logging.Level;",
          "import org.checkerframework.checker.formatter.qual.FormatMethod;",
          "public class UsesFormatMethod {",
          "  @FormatMethod",
          "  void log(Level level, String format, Object... args) {",
          "    System.out.println(String.format(format, args));",
          "  }",
          "  void notAFormatMethod(Level level, String control) {",
          "    System.out.println(control);",
          "  }",
          "  void use() {",
          "    log(Level.INFO, \"%s\", \"hello\");",
          "    log(Level.INFO, \"%d\", 42);",
          "    notAFormatMethod(Level.INFO, \"%d\");",
          "  }",
          "}");

  /** The JVML signature of method {@code log}. */
  private static final String LOG_SIGNATURE =
      "log(Ljava/util/logging/Level;Ljava/lang/String;[Ljava/lang/Object;)V";

  /** The JVML signature of method {@code notAFormatMethod}. */
  private static final String NOT_A_FORMAT_METHOD_SIGNATURE =
      "notAFormatMethod(Ljava/util/logging/Level;Ljava/lang/String;)V";

  @Test
  public void noFormatAnnotationInAjavaFile() throws IOException {
    String ajava = runInference("ajava", ".ajava");

    // The control shows that inference would have annotated a `String` parameter of this method.
    Assert.assertTrue(
        "Whole-program inference did not infer @Format for the String parameter of a method that"
            + " is not a format method, so this test does not test anything; the generated ajava"
            + " file is:"
            + System.lineSeparator()
            + ajava,
        ajava.matches("(?s).*Format\\(\\{[^}]*\\}\\)\\s+String control.*"));

    Assert.assertFalse(
        "Whole-program inference inferred @Format for the format string parameter of a"
            + " @FormatMethod method; the generated ajava file is:"
            + System.lineSeparator()
            + ajava,
        ajava.matches("(?s).*Format\\(\\{[^}]*\\}\\)\\s+String format.*"));
  }

  @Test
  public void noFormatAnnotationInJaifFile() throws IOException {
    String jaif = runInference("jaifs", ".jaif");

    // The control shows that inference would have annotated a `String` parameter of this method.
    String controlAnnos = parameterAnnotations(jaif, NOT_A_FORMAT_METHOD_SIGNATURE, 1);
    Assert.assertTrue(
        "Whole-program inference did not infer @Format for the String parameter of a method that"
            + " is not a format method, so this test does not test anything; the generated jaif"
            + " file is:"
            + System.lineSeparator()
            + jaif,
        controlAnnos.contains("Format"));

    String formatStringAnnos = parameterAnnotations(jaif, LOG_SIGNATURE, 1);
    Assert.assertEquals(
        "Whole-program inference inferred an annotation for the format string parameter of a"
            + " @FormatMethod method; the generated jaif file is:"
            + System.lineSeparator()
            + jaif,
        "",
        formatStringAnnos);
  }

  /**
   * Returns the annotations that a .jaif file puts on a formal parameter: the text of the {@code
   * type:} line that follows the {@code parameter #index:} line of the given method.
   *
   * @param jaif the contents of a .jaif file
   * @param methodSignature the method's simple name followed by its erased JVML signature
   * @param index the 0-based index of a formal parameter
   * @return the annotations on the given formal parameter, or "" if there are none
   */
  private static String parameterAnnotations(String jaif, String methodSignature, int index) {
    List<String> lines = Arrays.asList(jaif.split("\\R"));
    int methodLine = -1;
    for (int i = 0; i < lines.size(); i++) {
      if (lines.get(i).trim().startsWith("method " + methodSignature + ":")) {
        methodLine = i;
        break;
      }
    }
    Assert.assertNotEquals(
        "No entry for method "
            + methodSignature
            + " in the generated jaif file:"
            + System.lineSeparator()
            + jaif,
        -1,
        methodLine);

    for (int i = methodLine + 1; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      if (line.startsWith("method ") || line.startsWith("class ")) {
        break;
      }
      if (line.equals("parameter #" + index + ":")) {
        String next = i + 1 < lines.size() ? lines.get(i + 1).trim() : "";
        return next.startsWith("type:") ? next.substring("type:".length()).trim() : "";
      }
    }
    return "";
  }

  /**
   * Runs whole-program inference, using the Formatter Checker, on {@link #SOURCE}.
   *
   * @param inferMode the argument to the {@code -Ainfer} command-line option
   * @param extension the file extension of the annotation file that {@code inferMode} produces
   * @return the contents of the generated annotation file
   * @throws IOException if a file cannot be read or written
   */
  private String runInference(String inferMode, String extension) throws IOException {
    File sourceFile =
        tempFolder.newFolder("src").toPath().resolve("UsesFormatMethod.java").toFile();
    Files.write(sourceFile.toPath(), SOURCE.getBytes(StandardCharsets.UTF_8));
    File classesDir = tempFolder.newFolder("classes");
    File inferenceDir = tempFolder.newFolder("inference-output");

    List<String> options =
        Arrays.asList(
            "-processor",
            FormatterChecker.class.getCanonicalName(),
            "-Ainfer=" + inferMode,
            "-AinferOutputDirectory=" + inferenceDir,
            // Do not let a type-checking error halt compilation before whole-program inference
            // writes its results.
            "-Awarns",
            "-ApermitMissingJdk",
            "-classpath",
            System.getProperty("java.class.path"),
            "-d",
            classesDir.toString());

    StringWriter javacOutput = new StringWriter();
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
      Iterable<? extends JavaFileObject> javaFiles =
          fileManager.getJavaFileObjects(new File[] {sourceFile});
      JavaCompiler.CompilationTask task =
          compiler.getTask(
              javacOutput, fileManager, null, options, Collections.emptyList(), javaFiles);
      if (!task.call()) {
        Assert.fail("Compilation failed:" + System.lineSeparator() + javacOutput);
      }
    }

    List<Path> annotationFiles;
    try (Stream<Path> files = Files.walk(inferenceDir.toPath())) {
      annotationFiles =
          files
              .filter(p -> p.getFileName().toString().endsWith(extension))
              .collect(Collectors.toList());
    }
    Assert.assertEquals(
        "Expected exactly one " + extension + " file, but found " + annotationFiles,
        1,
        annotationFiles.size());
    return new String(Files.readAllBytes(annotationFiles.get(0)), StandardCharsets.UTF_8);
  }
}
