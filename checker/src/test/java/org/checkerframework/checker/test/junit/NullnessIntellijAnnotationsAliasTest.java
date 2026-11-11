package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit test for the Nullness Checker with an IntelliJ IDEA annotations file that uses an alias
 * that is not on the classpath.
 */
public class NullnessIntellijAnnotationsAliasTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a NullnessIntellijAnnotationsAliasTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public NullnessIntellijAnnotationsAliasTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-AintellijAnnotations=tests/nullness-intellijannotations-alias",
        // Other checkers in the Nullness Checker's compound checker, which do not know the alias,
        // warn that the alias is an unknown annotation.
        "-AstubNoWarnIfNotFound");
  }

  /**
   * Returns the test directories for this test suite.
   *
   * @return array of test directory names
   */
  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-intellijannotations-alias"};
  }
}
