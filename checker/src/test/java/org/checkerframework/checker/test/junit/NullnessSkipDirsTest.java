package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness Checker -- testing {@code -AskipFiles} command-line argument. */
public class NullnessSkipDirsTest extends CheckerFrameworkPerDirectoryTest {

  public NullnessSkipDirsTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.nullness.NullnessChecker.class,
        "nullness",
        "-AskipFiles=/skip/this/.*");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nullness-skipdirs"};
  }
}
