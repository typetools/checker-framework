package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Library for the Nullness checker when using safe defaults for unannotated source code.
 */
public class NullnessSafeDefaultsSourceCodeLibTest extends CheckerFrameworkTest {

    public NullnessSafeDefaultsSourceCodeLibTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseDefaultsForUncheckedCode=source,bytecode",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-safedefaultssourcecodelib"};
    }
}
