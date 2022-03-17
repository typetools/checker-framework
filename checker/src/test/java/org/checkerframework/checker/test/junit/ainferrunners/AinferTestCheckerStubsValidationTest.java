package org.checkerframework.checker.test.junit.ainferrunners;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.testchecker.ainfer.AinferTestChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.CheckerFrameworkWPIPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests whole-program type inference with stub files. This test is the second pass, which ensures
 * that with the stubs in place, the errors that those annotations remove are no longer issued.
 */
@Category(AinferTestCheckerStubsTest.class)
public class AinferTestCheckerStubsValidationTest extends CheckerFrameworkWPIPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public AinferTestCheckerStubsValidationTest(List<File> testFiles) {
    super(
        testFiles,
        AinferTestChecker.class,
        "ainfer-testchecker/annotated",
        AinferTestCheckerStubsTest.class,
        "-Anomsgtext",
        astubsArgFromFiles(testFiles),
        // "-AstubDebug",
        "-AmergeStubsWithSource",
        "-Awarns",
        "-AskipDefs=TestPure");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"ainfer-testchecker/annotated/"};
  }
}
