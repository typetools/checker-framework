package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program inference with the aid of ajava files. This test is the first pass on the
 * test data, which generates the ajava files. This specific test suite is designed to elicit
 * problems with ajava parsing that only occur when the Nullness Checker is run.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-index/ are not relevant.
 * The meaning of this test class is to test if the generated ajava files are similar to the
 * expected ones. The errors on .java files must be ignored.
 */
@Category(AinferNullnessAjavaGenerationTest.class)
public class AinferNullnessAjavaGenerationTest extends AinferGeneratePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferNullnessAjavaGenerationTest(List<File> testFiles) {
    super(
        testFiles,
        NullnessChecker.class,
        "ainfer-nullness/non-annotated",
        "-Ainfer=ajava",
        "-Aajava=tests/ainfer-nullness/input-annotation-files/",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-nullness/non-annotated"};
  }
}
