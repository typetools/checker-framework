package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.toy.ToyChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class ToyTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public ToyTest(List<File> testFiles) {
        super(testFiles, ToyChecker.class, "toy", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"toy"};
    }
}
