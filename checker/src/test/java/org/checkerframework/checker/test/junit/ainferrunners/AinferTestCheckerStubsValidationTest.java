package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with stub files. This test is the second pass, which ensures
 * that with the stubs in place, the errors that those annotations remove are no longer issued.
 */
@Category(AinferTestCheckerStubsTest.class)
public class AinferTestCheckerStubsValidationTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public AinferTestCheckerStubsValidationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "ainfer-testchecker/annotated",
        "-Anomsgtext",
        "-Astubs=tests/ainfer-testchecker/inference-output",
        // "-AstubDebug",
        "-AmergeStubsWithSource",
        "-Awarns");
  }

  @Override
  public void run() {
    // Only run if annotated files have been created.
    // See ainferTests task.
    if (!new File("tests/ainfer-testchecker/annotated/").exists()) {
      throw new RuntimeException(
          AinferTestCheckerStubsTest.class + " must be run before this test.");
    }
    super.run();
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/annotated/"};
  }
}
