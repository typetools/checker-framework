package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests the constant value propagation type system without overflow. */
public class ValueIgnoreRangeOverflowTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public ValueIgnoreRangeOverflowTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.value.ValueChecker.class,
                "value",
                "-Anomsgtext",
                "-A" + ValueChecker.REPORT_EVAL_WARNS,
                "-A" + ValueChecker.IGNORE_RANGE_OVERFLOW);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"value", "all-systems", "value-ignore-range-overflow"};
    }
}
