package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.AinferGeneratePerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Runs whole-program inference and inserts annotations into source code.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-testchecker/ are not
 * relevant. The meaning of this test class is to test if the generated .jaif files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(AinferTestCheckerJaifsGenerationTest.class)
public class AinferTestCheckerJaifsGenerationTest extends AinferGeneratePerDirectoryTest {
  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public AinferTestCheckerJaifsGenerationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "ainfer-testchecker/non-annotated",
        "-Ainfer=jaifs",
        // Use a stub file here, even though this is a JAIF test. This test can't pass
        // without an external file that specifies that a method is pure, and there is no
        // way to directly pass a JAIF file (in a real WPI run, the JAIF file's annotations
        // would have been inserted into the source).
        "-Astubs=tests/ainfer-testchecker/input-annotation-files/ExistingPurityAnnotations-org.checkerframework.checker.testchecker.ainfer.AinferTestChecker.astub",
        "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/non-annotated"};
  }
}
