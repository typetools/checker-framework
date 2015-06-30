package tests;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker.
 */
public class NullnessTempTest extends ParameterizedCheckerTest {

    public NullnessTempTest(File testFile) {
        // TODO: remove forbidnonnullarraycomponents option once it's no
        // longer needed.  See issues 154, 322, and 433.
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-Alint=forbidnonnullarraycomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-temp");
    }

}
