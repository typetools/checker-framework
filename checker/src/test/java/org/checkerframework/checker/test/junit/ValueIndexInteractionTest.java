package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Value Checker's interactions with the Index Checker. */
public class ValueIndexInteractionTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a ValueIndexInteractionTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public ValueIndexInteractionTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.common.value.ValueChecker.class,
        "value-index-interaction",
        "-Anomsgtext");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"value-index-interaction"};
  }
}
