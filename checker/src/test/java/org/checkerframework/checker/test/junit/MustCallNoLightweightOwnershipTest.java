package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class MustCallNoLightweightOwnershipTest extends CheckerFrameworkPerDirectoryTest {
  public MustCallNoLightweightOwnershipTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.checker.mustcall.MustCallChecker.class,
        "mustcall-nolightweightownership",
        "-Anomsgtext",
        "-AnoLightweightOwnership",
        // "-AstubDebug");
        "-nowarn");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"mustcall-nolightweightownership"};
  }
}
