package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the compound checker design pattern
 *
 * @author smillst
 *
 */
public class CompoundCheckerTest extends CheckerFrameworkTest {

    public CompoundCheckerTest(File testFile) {
        super(testFile, tests.compound.CompoundChecker.class, "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compound-checker"};
    }
}
