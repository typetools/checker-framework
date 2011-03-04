package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LubGlbTest extends ParameterizedCheckerTest {

    public LubGlbTest(File testFile) {
        super(testFile, "lubglb.LubGlbChecker", "lubglb", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("lubglb"); }
}