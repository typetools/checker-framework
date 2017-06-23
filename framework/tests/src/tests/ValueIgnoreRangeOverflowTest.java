package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the constant value propagation type system without overflow.
 *
 * @author kelloggm
 */
public class ValueIgnoreRangeOverflowTest extends CheckerFrameworkPerDirectoryTest {

    public ValueIgnoreRangeOverflowTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.value.ValueChecker.class,
                "value",
                "-Anomsgtext",
                "-Astubs=statically-executable.astub",
                "-A" + ValueChecker.REPORT_EVAL_WARNS,
                "-A" + ValueChecker.IGNORE_RANGE_OVERFLOW);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"value", "all-systems", "value-ignore-range-overflow"};
    }
}
