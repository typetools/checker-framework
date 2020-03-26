package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class NullnessStubfileTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NullnessStubfileTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AstubWarnIfNotFound",
                "-Astubs="
                        + "tests/nullness-stubfile/stubfile1.astub:"
                        + "tests/nullness-stubfile/stubfile2.astub");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-stubfile"};
    }
}
