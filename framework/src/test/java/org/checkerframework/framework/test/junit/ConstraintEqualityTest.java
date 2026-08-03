package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.constraintequality.ConstraintEqualityChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Runs the {@link ConstraintEqualityChecker}, which tests {@code Qualifier.equals} and {@code
 * QualifierTyping.equals}, and their {@code hashCode} methods. The test files are irrelevant; the
 * checker performs its tests on the first class it visits.
 */
public class ConstraintEqualityTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ConstraintEqualityTest(List<File> testFiles) {
    super(testFiles, ConstraintEqualityChecker.class, "simple");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"simple"};
  }
}
