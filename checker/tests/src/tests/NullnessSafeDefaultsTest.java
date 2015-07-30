package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker when using safe defaults for unannotated bytecode.
 */
public class NullnessSafeDefaultsTest extends ParameterizedCheckerTest {

    public NullnessSafeDefaultsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AsafeDefaultsForUnannotatedBytecode",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-safedefaults");
    }

}
