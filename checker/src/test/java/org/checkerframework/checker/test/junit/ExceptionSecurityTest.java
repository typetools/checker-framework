package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

/** Testing Exception Security Checker */
public class ExceptionSecurityTest extends CheckerFrameworkPerDirectoryTest {

  public ExceptionSecurityTest(List<File> testFiles) {
    super(testFiles, org.checkerframework.checker.err01.ExceptionSecurityChecker.class, "err01");
  }

  @Parameterized.Parameters
  public static String[] getTestDirs() {
    return new String[] {"err01"};
  }
}
