package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker when using safe defaults for unannotated source code.
 */
public class NullnessSafeDefaultsSourceCodeTest extends CheckerFrameworkTest {

    public NullnessSafeDefaultsSourceCodeTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseDefaultsForUncheckedCode=source",
                // This test reads bytecode .class files created by NullnessSafeDefaultsSourceCodeLibTest
                "-cp",
                "dist/checker.jar:tests/build/testclasses/",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-safedefaultssourcecode"};
    }
}
