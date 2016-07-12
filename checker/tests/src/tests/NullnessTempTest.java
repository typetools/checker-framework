package tests;

import java.io.File;
import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker.
 */
public class NullnessTempTest extends CheckerFrameworkTest {

    public NullnessTempTest(File testFile) {
        // TODO: remove forbidnonnullarraycomponents option once it's no
        // longer needed.  See issues 154, 322, and 433.
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-Alint=forbidnonnullarraycomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-temp"};
    }
}
