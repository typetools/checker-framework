package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ValueNullStringsConcatenationTest extends FrameworkPerDirectoryTest {

    public ValueNullStringsConcatenationTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.common.value.ValueChecker.class,
                "value-null-strings-concatenation",
                "-Anomsgtext",
                "-Astubs=statically-executable.astub",
                "-A" + ValueChecker.REPORT_EVAL_WARNS,
                "-A" + ValueChecker.NULL_STRINGS_CONCATENATION);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"all-systems", "value-null-strings-concatenation"};
    }
}
