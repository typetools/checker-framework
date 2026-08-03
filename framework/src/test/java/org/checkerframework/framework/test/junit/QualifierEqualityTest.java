package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.qualifierequality.QualifierEqualityChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the {@code equals} and {@code hashCode} methods of the qualifier classes that type argument
 * inference uses. The tests are performed within the {@link QualifierEqualityChecker}, which throws
 * an {@code AssertionError} if a test fails.
 */
public class QualifierEqualityTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public QualifierEqualityTest(List<File> testFiles) {
    super(testFiles, QualifierEqualityChecker.class, "qualifierequality");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"qualifierequality"};
  }
}
