package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.nontopdefault.NTDChecker;

/**
 * Tests the NonTopDefault Checker.
 *
 * @author jyluo
 */
public class NonTopDefaultTest extends CheckerFrameworkPerDirectoryTest {

    public NonTopDefaultTest(List<File> testFiles) {
        super(testFiles, NTDChecker.class, "nontopdefault", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nontopdefault"};
    }
}
