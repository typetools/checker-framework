package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the compound checker design pattern
 *
 * @author smillst
 */
public class CompoundCheckerTest extends CheckerFrameworkPerDirectoryTest {

    public CompoundCheckerTest(List<File> testFiles) {
        super(testFiles, tests.compound.CompoundChecker.class, "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compound-checker"};
    }
}
