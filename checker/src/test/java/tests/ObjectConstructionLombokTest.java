package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Test that the Object Construction Checker's support for Lombok works correctly. */
public class ObjectConstructionLombokTest extends CheckerFrameworkPerDirectoryTest {
    public ObjectConstructionLombokTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
                "objectconstruction-lombok",
                "-Anomsgtext",
                "-nowarn",
                "-AsuppressWarnings=type.anno.before.modifier");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"objectconstruction-lombok"};
    }
}
