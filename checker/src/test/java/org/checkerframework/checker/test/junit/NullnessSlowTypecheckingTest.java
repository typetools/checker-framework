package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Regression test for type-checking performance. Runs the Nullness Checker with {@code
 * -AslowTypecheckingSeconds} set low enough that a "slow typechecking" warning is an unexpected
 * diagnostic, so that a performance regression fails this test rather than merely being logged.
 *
 * <p>Test inputs in {@code nullness-slow-typechecking/} are files that are known to be slow to
 * type-check unless a specific performance issue is fixed. Each test input file should document, in
 * a comment, which performance issue it guards against.
 *
 * <p>The threshold applies to every file in the directory, so it is set by the slowest one. Choose
 * it from measurements taken <em>through this test</em>, not from the command line: {@code
 * slow.typechecking} measures wall-clock time, and this test runs while the rest of the build does,
 * so the same file reports roughly twice as much here as it does in an otherwise idle {@code java
 * -jar checker.jar} run. Measured for {@code Issue7023.java}: 5-7 seconds from the command line but
 * 12 through this test, and 19-31 through this test without the fix it guards.
 */
public class NullnessSlowTypecheckingTest extends CheckerFrameworkPerDirectoryTest {

  public NullnessSlowTypecheckingTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness-slow-typechecking",
        "-AslowTypecheckingSeconds=15");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-slow-typechecking"};
  }
}
