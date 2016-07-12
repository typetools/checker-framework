package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Library for the Nullness checker for issue #511.
 */
public class NullnessGenericWildcardLibTest extends CheckerFrameworkTest {

    public NullnessGenericWildcardLibTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-genericwildcardlib"};
    }
}
