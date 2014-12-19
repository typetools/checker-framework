package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker.
 */
public class NullnessTempTest extends ParameterizedCheckerTest {

    public NullnessTempTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.  See issues 154 and 322.
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-temp");
    }

}
