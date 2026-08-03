package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.typeinference8.Typeinference8InvariantChecker;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests that the invariants of package {@code org.checkerframework.framework.util.typeinference8}
 * are enforced even when Java assertions are disabled. The tests themselves are performed by {@link
 * org.checkerframework.framework.testchecker.typeinference8.Typeinference8InvariantVisitor}.
 */
public class Typeinference8InvariantTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * @param testFiles the files containing test code, which will be type-checked
   */
  public Typeinference8InvariantTest(List<File> testFiles) {
    super(testFiles, Typeinference8InvariantChecker.class, "typeinference8invariant");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"typeinference8invariant"};
  }
}
