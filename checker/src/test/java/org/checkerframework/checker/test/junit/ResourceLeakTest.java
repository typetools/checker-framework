package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the Resource Leak Checker. */
public class ResourceLeakTest extends CheckerFrameworkPerDirectoryTest {
  public ResourceLeakTest(List<File> testFiles) {
    super(
        testFiles,
        ResourceLeakChecker.class,
        "resourceleak",
        "-Anomsgtext",
        "-AwarnUnneededSuppressions",
        "-encoding",
        "UTF-8");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"resourceleak"};
  }
}
