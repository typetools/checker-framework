package org.checkerframework.framework.test.junit;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.testchecker.aggregate.AggregateOfCompoundChecker;
import org.junit.runners.Parameterized.Parameters;

public class AggregateTest extends CheckerFrameworkPerFileTest {

  /**
   * @param file the file containing test code, which will be type-checked
   */
  public AggregateTest(File file) {
    super(file, AggregateOfCompoundChecker.class, "aggregate", "-AresolveReflection");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"aggregate"};
  }
}
