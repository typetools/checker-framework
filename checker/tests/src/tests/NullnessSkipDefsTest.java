package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness Checker -- testing -AskipDefs command-line argument.
 */
public class NullnessSkipDefsTest extends ParameterizedCheckerTest {

    public NullnessSkipDefsTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext", "-AskipDefs=SkipMe");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-skipdefs");
    }

}
