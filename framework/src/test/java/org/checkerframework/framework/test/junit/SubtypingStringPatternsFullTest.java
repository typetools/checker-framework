package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test suite for the Subtyping Checker, using {@code @QualifierForLiterals}. */
public class SubtypingStringPatternsFullTest extends CheckerFrameworkPerDirectoryTest {

  /** @param testFiles the files containing test code, which will be type-checked */
  public SubtypingStringPatternsFullTest(List<File> testFiles) {
    super(
        testFiles,
        org.checkerframework.common.subtyping.SubtypingChecker.class,
        "stringpatterns/stringpatterns-full",
        "-Anomsgtext",
        "-Aquals=org.checkerframework.framework.testchecker.util.PatternUnknown,org.checkerframework.framework.testchecker.util.PatternAB,org.checkerframework.framework.testchecker.util.PatternBC,org.checkerframework.framework.testchecker.util.PatternAC,org.checkerframework.framework.testchecker.util.PatternA,org.checkerframework.framework.testchecker.util.PatternB,org.checkerframework.framework.testchecker.util.PatternC,org.checkerframework.framework.testchecker.util.PatternBottomFull");
  }

  @Parameters
  public static String[] getTestDirs() {
    return new String[] {"stringpatterns/stringpatterns-full", "all-systems"};
  }
}
