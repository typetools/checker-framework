package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 */
public class LubGlbTest extends ParameterizedCheckerTest {

    public LubGlbTest(File testFile) {
        super(testFile, lubglb.LubGlbChecker.class.getName(), "lubglb",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("lubglb"); }
}