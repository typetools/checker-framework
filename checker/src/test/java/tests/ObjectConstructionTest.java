package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Basic tests for the Object Construction Checker. */
public class ObjectConstructionTest extends CheckerFrameworkPerDirectoryTest {
    public ObjectConstructionTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
                "objectconstruction",
                "-Anomsgtext",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"objectconstruction"};
    }
}
