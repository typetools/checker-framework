package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NoLightweightOwnershipTest extends CheckerFrameworkPerDirectoryTest {
  public NoLightweightOwnershipTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.mustcall.MustCallChecker.class,
        "nolightweightownership",
        "-Anomsgtext",
        "-AnoLightweightOwnership",
        // "-AstubDebug");
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"nolightweightownership"};
  }
}
