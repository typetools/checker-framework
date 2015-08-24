package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

/**
 * JUnit tests for the Nullness checker when running with concurrent semantics
 */
public class NullnessConcurrentTest extends DefaultCheckerTest {

    public NullnessConcurrentTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AconcurrentSemantics",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"nullness-concurrent-semantics"};
    }

}
