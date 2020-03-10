package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.purity.PurityChecker;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Tests for the {@code -AcheckPurityAnnotations} command-line argument. */
public class PurityTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public PurityTest(List<File> testFiles) {
        super(testFiles, PurityChecker.class, "purity", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"purity"};
    }
}
