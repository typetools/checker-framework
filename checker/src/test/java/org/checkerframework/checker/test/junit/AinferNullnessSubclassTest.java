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
import org.checkerframework.checker.testchecker.nullnesssubclass.NullnessSubclassChecker;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Tests that whole-program inference infers from assignments of {@code null} not only for the
 * Nullness Checker itself, but also for a checker that is built on top of the Nullness Checker.
 */
public class AinferNullnessSubclassTest {

  /** The directories that this test writes into. */
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  /** Creates a new AinferNullnessSubclassTest. */
  public AinferNullnessSubclassTest() {}

  /** The source code on which inference is run. */
  private static final String SOURCE =
      String.join(
          System.lineSeparator(),
          "package wpitest;",
          "public class AssignsNull {",
          "  Object field = new Object();",
          "  void setFieldToNull() {",
          "    field = null;",
          "  }",
          "}");

  @Test
  public void inferNullableFromNullAssignment() throws IOException {
    String ajava = runInference();
    Assert.assertTrue(
        "Whole-program inference did not infer @Nullable for a field that is assigned null;"
            + " the generated ajava file is:"
            + System.lineSeparator()
            + ajava,
        ajava.contains("Nullable"));
  }

  /**
   * Runs whole-program inference, in ajava mode, on {@link #SOURCE}, using a checker that is a
   * subclass of the Nullness Checker.
   *
   * @return the contents of the generated ajava file
   * @throws IOException if a file cannot be read or written
   */
  private String runInference() throws IOException {
    File sourceFile = tempFolder.newFolder("src").toPath().resolve("AssignsNull.java").toFile();
    Files.write(sourceFile.toPath(), SOURCE.getBytes(StandardCharsets.UTF_8));
    File classesDir = tempFolder.newFolder("classes");
    File inferenceDir = tempFolder.newFolder("inference-output");

    List<String> options =
        Arrays.asList(
            "-processor",
            NullnessSubclassChecker.class.getCanonicalName(),
            "-Ainfer=ajava",
            "-AinferOutputDirectory=" + inferenceDir,
            // Do not let type-checking errors, which are expected, halt compilation before
            // whole-program inference writes its results.
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

    // Each checker in the hierarchy writes its own ajava file; read the one for the checker
    // whose type factory is a subclass of NullnessAnnotatedTypeFactory.
    String ajavaFileSuffix = "-" + NullnessSubclassChecker.class.getCanonicalName() + ".ajava";
    List<Path> ajavaFiles;
    try (Stream<Path> files = Files.walk(inferenceDir.toPath())) {
      ajavaFiles =
          files
              .filter(p -> p.getFileName().toString().endsWith(ajavaFileSuffix))
              .collect(Collectors.toList());
    }
    Assert.assertEquals(
        "Expected exactly one ajava file ending with "
            + ajavaFileSuffix
            + ", but found "
            + ajavaFiles,
        1,
        ajavaFiles.size());
    return new String(Files.readAllBytes(ajavaFiles.get(0)), StandardCharsets.UTF_8);
  }
}
