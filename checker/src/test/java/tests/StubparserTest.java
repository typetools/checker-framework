package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class StubparserTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a StubparserTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public StubparserTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AstubWarnIfNotFound");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"stubparser-tests"};
    }
}
