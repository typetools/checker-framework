package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Runs whole-program inference and inserts annotations into source code.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/ainfer-nullness/ are not relevant.
 * The meaning of this test class is to test if the generated .jaif files are similar to the
 * expected ones. The errors on .java files must be ignored.
 */
@Category(WholeProgramInferenceNullnessJaifsTest.class)
public class WholeProgramInferenceNullnessJaifsTest extends CheckerFrameworkPerDirectoryTest {
  /** @param testFiles the files containing test code, which will be type-checked */
  public WholeProgramInferenceNullnessJaifsTest(List<File> testFiles) {
    super(testFiles, NullnessChecker.class, "nullness", "-Anomsgtext", "-Ainfer=jaifs", "-Awarns");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-nullness/non-annotated"};
  }
}
