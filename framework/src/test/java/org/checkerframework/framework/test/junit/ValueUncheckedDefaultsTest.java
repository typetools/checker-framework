package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests conservative defaults for the constant value propagation type system. */
public class ValueUncheckedDefaultsTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public ValueUncheckedDefaultsTest(List<File> testFiles) {
    super(
        testFiles,
        ValueChecker.class,
        "value",
        "-Anomsgtext",
        "-AuseConservativeDefaultsForUncheckedCode=btyecode",
        "-A" + ValueChecker.REPORT_EVAL_WARNS);
  }

  @Parameters
  public static String[] getTestDirs() {
    // The defaults for unchecked code should be the same as checked code, so use the same
    // tests.
    return new String[] {"value", "all-systems"};
  }
}
