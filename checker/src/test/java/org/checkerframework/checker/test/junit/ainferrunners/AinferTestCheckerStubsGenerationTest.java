package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program inference with the aid of stub files. This test is the first pass on the test
 * data, which generates the stubs.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-testchecker/ are not
 * relevant. The meaning of this test class is to test if the generated stub files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(AinferTestCheckerStubsGenerationTest.class)
public class AinferTestCheckerStubsGenerationTest extends AinferGeneratePerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferTestCheckerStubsGenerationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "ainfer-testchecker/non-annotated",
        "-Ainfer=stubs",
        "-Astubs=tests/ainfer-testchecker/input-annotation-files/ExistingPurityAnnotations-org.checkerframework.checker.testchecker.ainfer.AinferTestChecker.astub",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/non-annotated"};
  }
}
