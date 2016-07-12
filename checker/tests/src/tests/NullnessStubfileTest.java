package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class NullnessStubfileTest extends CheckerFrameworkTest {

    public NullnessStubfileTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AprintErrorStack",
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
