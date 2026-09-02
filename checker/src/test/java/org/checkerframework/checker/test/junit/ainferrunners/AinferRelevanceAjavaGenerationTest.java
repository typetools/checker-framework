package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferRelevanceTestChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
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
}
