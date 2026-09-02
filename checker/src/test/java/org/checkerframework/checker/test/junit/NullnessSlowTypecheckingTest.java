package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Regression test for the performance of {@code DefaultTypeHierarchy#isSubtype}. Runs the Nullness
 * Checker with {@code -AslowTypecheckingSeconds} set low enough that a "slow typechecking" warning
 * is an unexpected diagnostic, so that a performance regression fails this test rather than merely
 * being logged.
 *
 * <p>Test inputs in {@code nullness-slow-typechecking/} are files that are known to be slow to
 * type-check unless a specific performance issue is fixed. Each test input file should document, in
 * a comment, which performance issue it guards against.
 */
public class NullnessSlowTypecheckingTest extends CheckerFrameworkPerDirectoryTest {

  public NullnessSlowTypecheckingTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness-slow-typechecking",
        "-AslowTypecheckingSeconds=20");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-slow-typechecking"};
  }
}
