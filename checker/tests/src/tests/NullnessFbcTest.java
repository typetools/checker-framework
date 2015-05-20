package tests;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker (that uses the Freedom Before Commitment
 * type system for initialization).
 */
public class NullnessFbcTest extends ParameterizedCheckerTest {

    public NullnessFbcTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.  See issues 154 and 322:
        // https://code.google.com/p/checker-framework/issues/detail?id=154
        // https://code.google.com/p/checker-framework/issues/detail?id=322
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckPurityAnnotations", "-AunsafeUntyped",
                "-Anomsgtext", "-Xlint:deprecation",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness", "initialization/fbc", "all-systems");
    }

}
