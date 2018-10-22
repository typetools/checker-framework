package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.nontopdefault.NTDChecker;

/** Tests the NonTopDefault Checker. */
public class NonTopDefaultTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NonTopDefaultTest(List<File> testFiles) {
        super(testFiles, NTDChecker.class, "nontopdefault", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nontopdefault"};
    }
}
