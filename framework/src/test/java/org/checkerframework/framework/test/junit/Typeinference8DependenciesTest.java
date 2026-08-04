package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typeinference8dependencies.DependenciesChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for {@link org.checkerframework.framework.util.typeinference8.types.Dependencies}.
 */
public class Typeinference8DependenciesTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public Typeinference8DependenciesTest(List<File> testFiles) {
    super(testFiles, DependenciesChecker.class, "typeinference8dependencies");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"typeinference8dependencies"};
  }
}
