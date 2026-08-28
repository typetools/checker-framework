package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Regression test for the performance of {@code ValueQualifierHierarchy}'s LUB and GLB. Runs the
 * Value Checker with {@code -AslowTypecheckingSeconds} set low enough that a "slow typechecking"
 * warning is an unexpected diagnostic, so that a performance regression fails this test rather
 * than merely being logged.
 *
 * <p>Test inputs in {@code value-slow-typechecking/} are files that are known to be slow to
 * type-check unless a specific performance issue is fixed. Each test input file should document, in
 * a comment, which performance issue it guards against.
 */
public class ValueSlowTypecheckingTest extends CheckerFrameworkPerDirectoryTest {

  public ValueSlowTypecheckingTest(List<File> testFiles) {
    super(testFiles, ValueChecker.class, "value-slow-typechecking", "-AslowTypecheckingSeconds=12");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"value-slow-typechecking"};
  }
}
