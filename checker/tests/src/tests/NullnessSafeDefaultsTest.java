package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker when using safe defaults for unannotated bytecode.
 */
public class NullnessSafeDefaultsTest extends DefaultCheckerTest {

    public NullnessSafeDefaultsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AsafeDefaultsForUnannotatedBytecode",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-safedefaults"};
    }

}
