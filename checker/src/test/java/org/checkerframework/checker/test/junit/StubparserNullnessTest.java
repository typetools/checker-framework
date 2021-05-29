package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Tests for stub parsing. */
public class StubparserNullnessTest extends CheckerFrameworkPerDirectoryTest {

  /**
   * Create a StubparserNullnessTest.
   *
   * @param testFiles the files containing test code, which will be type-checked
   */
  public StubparserNullnessTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "stubparser-nullness",
        "-Anomsgtext",
        "-Astubs=tests/stubparser-nullness",
        "-AstubWarnIfNotFound");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"stubparser-nullness"};
  }
}
