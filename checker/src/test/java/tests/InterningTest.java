package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Interning Checker, which tests the Interned annotation. */
public class InterningTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public InterningTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.interning.InterningChecker.class,
                "interning",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"interning", "all-systems"};
    }
}
