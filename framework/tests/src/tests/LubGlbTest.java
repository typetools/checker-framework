package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class LubGlbTest extends ParameterizedCheckerTest {

    public LubGlbTest(File testFile) {
        super(testFile,
                lubglb.LubGlbChecker.class,
                "lubglb",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("lubglb");
    }
}