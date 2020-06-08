package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ValueNonNullStringsConcatenationTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public ValueNonNullStringsConcatenationTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.value.ValueChecker.class,
                "value-non-null-strings-concatenation",
                "-Anomsgtext",
                "-Astubs=statically-executable.astub",
                "-A" + ValueChecker.REPORT_EVAL_WARNS,
                "-A" + ValueChecker.NON_NULL_STRINGS_CONCATENATION);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"all-systems", "value-non-null-strings-concatenation"};
    }
}
