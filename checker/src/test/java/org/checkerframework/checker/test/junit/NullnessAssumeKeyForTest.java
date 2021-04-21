package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker. */
public class NullnessAssumeKeyForTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessAssumeKeyForTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessAssumeKeyForTest(List<File> testFiles) {
    // TODO: remove soundArrayCreationNullness option once it's no
    // longer needed.  See issue #986:
    // https://github.com/typetools/checker-framework/issues/986
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-AcheckPurityAnnotations",
        "-AassumeKeyFor",
        "-Anomsgtext",
        "-Xlint:deprecation",
        "-Alint=soundArrayCreationNullness," + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-assumekeyfor"};
  }
}
