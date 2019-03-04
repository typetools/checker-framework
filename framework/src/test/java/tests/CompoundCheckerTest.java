package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.compound.CompoundChecker;

/** Tests for the compound checker design pattern. */
public class CompoundCheckerTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public CompoundCheckerTest(List<File> testFiles) {
        super(testFiles, CompoundChecker.class, "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compound-checker"};
    }
}
