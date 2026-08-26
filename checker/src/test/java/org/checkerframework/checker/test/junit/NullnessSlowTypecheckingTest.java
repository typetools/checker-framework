package org.checkerframework.checker.test.junit;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
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
public class NullnessSlowTypecheckingTest extends CheckerFrameworkPerFileTest {

  public NullnessSlowTypecheckingTest(File testFile) {
    super(
        testFile,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness-slow-typechecking",
        "-AslowTypecheckingSeconds=50");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-slow-typechecking"};
  }
}
