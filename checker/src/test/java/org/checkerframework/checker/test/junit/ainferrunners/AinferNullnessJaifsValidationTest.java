package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with the aid of .jaif files. This test is the second pass,
 * which ensures that with the annotations inserted, the errors are no longer issued.
 */
@Category(AinferNullnessJaifsTest.class)
public class AinferNullnessJaifsValidationTest extends CheckerFrameworkPerDirectoryTest {
  /** @param testFiles the files containing test code, which will be type-checked */
  public AinferNullnessJaifsValidationTest(List<File> testFiles) {
    super(testFiles, NullnessChecker.class, "nullness", "-Anomsgtext");
  }

  @Override
  public void run() {
    // Only run if annotated files have been created.
    // See ainferTests task.
    if (!new File("tests/ainfer-nullness/annotated/").exists()) {
      throw new RuntimeException(AinferNullnessJaifsTest.class + " must be run before this test.");
    }
    super.run();
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-nullness/annotated/"};
  }
}
