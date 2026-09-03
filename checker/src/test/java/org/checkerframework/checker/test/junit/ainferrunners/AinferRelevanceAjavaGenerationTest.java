package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

  /**
   * Compares each generated ajava file to its goal file, if the goal file exists. A goal file is
   * named {@code <ClassName>.ajava.goal}.
   *
   * <p>Unlike the second (validation) pass of this test, this comparison detects an annotation that
   * inference wrote even though the annotation is irrelevant where it appears. Such an annotation
   * clutters the ajava file, but it does not change the result of type-checking, so no diagnostic
   * would reveal it.
   */
  @AfterClass
  public static void compareToGoalFiles() throws IOException {
    try (Stream<Path> goalFiles = Files.list(goalDir)) {
      goalFiles
          .filter(goalFile -> goalFile.getFileName().toString().endsWith(goalSuffix))
          .forEach(AinferRelevanceAjavaGenerationTest::compareToGoalFile);
    }
  }

  /**
   * Compares the generated ajava file that corresponds to the given goal file, to the goal file.
   *
   * @param goalFile a goal file
   */
  private static void compareToGoalFile(Path goalFile) {
    String goalFileName = goalFile.getFileName().toString();
    String className = goalFileName.substring(0, goalFileName.length() - goalSuffix.length());
    Path ajavaFile =
        inferenceOutputDir.resolve(
            className + "-" + AinferRelevanceTestChecker.class.getCanonicalName() + ".ajava");
    String goalContents;
    String ajavaContents;
    try {
      goalContents = Files.readString(goalFile, StandardCharsets.UTF_8);
      ajavaContents = Files.readString(ajavaFile, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    Assert.assertEquals(
        String.format(
            "%s differs from %s.  If the difference is desirable, overwrite the goal file:%n"
                + "  cp %s %s%n",
            ajavaFile.toAbsolutePath(),
            goalFile.toAbsolutePath(),
            ajavaFile.toAbsolutePath(),
            goalFile.toAbsolutePath()),
        goalContents,
        ajavaContents);
  }
}
