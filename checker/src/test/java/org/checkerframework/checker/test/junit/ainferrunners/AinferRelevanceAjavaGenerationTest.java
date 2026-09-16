package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.checkerframework.checker.testchecker.ainfer.AinferRelevanceTestChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program inference with the aid of ajava files, for a checker that declares
 * {@code @RelevantJavaTypes}. This test is the first pass on the test data, which generates the
 * ajava files.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-relevance/ are not
 * relevant. The meaning of this test class is to test if the generated ajava files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(AinferRelevanceAjavaGenerationTest.class)
public class AinferRelevanceAjavaGenerationTest extends AinferGeneratePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferRelevanceAjavaGenerationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferRelevanceTestChecker.class,
        "ainfer-relevance/non-annotated",
        "-Ainfer=ajava",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-relevance/non-annotated"};
  }

  /** The directory that contains the goal files. */
  private static final Path goalDir = Path.of("tests", "ainfer-relevance");

  /**
   * The directory into which inference writes the ajava files. (The {@code
   * ainferRelevanceGenerateAjava} Gradle task later renames this directory to {@code
   * tests/ainfer-relevance/inference-output}.)
   */
  private static final Path inferenceOutputDir = Path.of("build", "whole-program-inference");

  /** The suffix of a goal file's name. */
  private static final String goalSuffix = ".ajava.goal";

  /** The suffix of a generated ajava file's name. */
  private static final String ajavaSuffix =
      "-" + AinferRelevanceTestChecker.class.getCanonicalName() + ".ajava";

  /**
   * Compares each generated ajava file to its goal file. A goal file is named {@code
   * <ClassName>.ajava.goal}. Every generated ajava file must have a goal file and vice versa, so
   * that no inference result goes unexamined.
   *
   * <p>Unlike the second (validation) pass of this test, this comparison detects an annotation that
   * inference wrote even though the annotation is irrelevant where it appears. Such an annotation
   * clutters the ajava file, but it does not change the result of type-checking, so no diagnostic
   * would reveal it.
   *
   * @throws IOException if a file cannot be listed or read
   */
  @AfterClass
  public static void compareToGoalFiles() throws IOException {
    if (!Files.isDirectory(inferenceOutputDir)) {
      Assert.fail(
          String.format(
              "Inference created no directory %s, so it wrote no ajava file.%n",
              inferenceOutputDir.toAbsolutePath()));
    }

    // Goal files that no generated ajava file corresponds to.  Entries are removed below.
    SortedSet<Path> goalFilesWithoutAjavaFile = new TreeSet<>(goalFiles());

    for (Path ajavaFile : generatedAjavaFiles()) {
      Path goalFile = goalFileFor(ajavaFile);
      goalFilesWithoutAjavaFile.remove(goalFile);
      compareToGoalFile(ajavaFile, goalFile);
    }

    if (!goalFilesWithoutAjavaFile.isEmpty()) {
      Assert.fail(
          String.format(
              "Inference wrote no ajava file for these goal files:  %s%n"
                  + "Either inference inferred nothing for those classes, or the ajava files have"
                  + " unexpected names.  If inferring nothing is desirable, delete the goal"
                  + " files.%n",
              goalFilesWithoutAjavaFile));
    }
  }

  /**
   * Returns the goal files: the files in {@link #goalDir} whose names end in {@link #goalSuffix}.
   *
   * @return the goal files
   * @throws IOException if {@link #goalDir} cannot be listed
   */
  private static List<Path> goalFiles() throws IOException {
    try (Stream<Path> files = Files.list(goalDir)) {
      return files
          .filter(file -> file.getFileName().toString().endsWith(goalSuffix))
          .collect(Collectors.toList());
    }
  }

  /**
   * Returns the ajava files that inference wrote for {@link AinferRelevanceTestChecker}. The search
   * is recursive, because inference writes a class's ajava file into a subdirectory that
   * corresponds to the class's package.
   *
   * @return the generated ajava files
   * @throws IOException if {@link #inferenceOutputDir} cannot be walked
   */
  private static List<Path> generatedAjavaFiles() throws IOException {
    try (Stream<Path> files = Files.walk(inferenceOutputDir)) {
      return files
          .filter(file -> file.getFileName().toString().endsWith(ajavaSuffix))
          .sorted()
          .collect(Collectors.toList());
    }
  }

  /**
   * Returns the goal file that corresponds to the given generated ajava file. The goal file need
   * not exist.
   *
   * @param ajavaFile a generated ajava file
   * @return the goal file that corresponds to {@code ajavaFile}
   */
  private static Path goalFileFor(Path ajavaFile) {
    if (!inferenceOutputDir.equals(ajavaFile.getParent())) {
      // Inference writes a class's ajava file into a subdirectory that corresponds to the class's
      // package, but the goal files are all in one directory, so a goal file for a class in a
      // named package could never match.  Every test input is in the unnamed package, so this
      // failure means that a new test input declares a package.
      Assert.fail(
          String.format(
              "%s is not directly in %s, so its class is not in the unnamed package.  Either put"
                  + " the test input in the unnamed package, or generalize"
                  + " AinferRelevanceAjavaGenerationTest to give each goal file a name that"
                  + " includes the package.%n",
              ajavaFile.toAbsolutePath(), inferenceOutputDir.toAbsolutePath()));
    }
    String ajavaFileName = ajavaFile.getFileName().toString();
    String className = ajavaFileName.substring(0, ajavaFileName.length() - ajavaSuffix.length());
    return goalDir.resolve(className + goalSuffix);
  }

  /**
   * Compares a generated ajava file to its goal file.
   *
   * @param ajavaFile a generated ajava file
   * @param goalFile the goal file that corresponds to {@code ajavaFile}
   */
  private static void compareToGoalFile(Path ajavaFile, Path goalFile) {
    String copyCommand =
        String.format("  cp %s %s%n", ajavaFile.toAbsolutePath(), goalFile.toAbsolutePath());
    if (!Files.exists(goalFile)) {
      // Requiring a goal file for every generated ajava file ensures that no inference result --
      // in particular, no annotation that inference wrote on an irrelevant type -- goes
      // unexamined.
      Assert.fail(
          String.format(
              "Goal file %s does not exist, but inference wrote %s.  Every generated ajava file"
                  + " needs a goal file.  If the generated file is correct, create the goal"
                  + " file:%n%s",
              goalFile.toAbsolutePath(), ajavaFile.toAbsolutePath(), copyCommand));
    }
    String goalContents;
    String ajavaContents;
    try {
      goalContents = Files.readString(goalFile, StandardCharsets.UTF_8);
      ajavaContents = Files.readString(ajavaFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    String message =
        String.format(
            "%s differs from %s.  If the difference is desirable, overwrite the goal file:%n%s",
            ajavaFile.toAbsolutePath(), goalFile.toAbsolutePath(), copyCommand);
    Assert.assertEquals(message, goalContents, ajavaContents);
  }
}
