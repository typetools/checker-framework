package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

/**
 * JUnit tests for the Nullness checker when running with concurrent semantics
 */
public class NullnessConcurrentTest extends ParameterizedCheckerTest {

    public NullnessConcurrentTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AconcurrentSemantics",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-concurrent-semantics");
    }

}
