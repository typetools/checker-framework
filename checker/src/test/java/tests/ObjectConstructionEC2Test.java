package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class ObjectConstructionEC2Test extends CheckerFrameworkPerDirectoryTest {
    public ObjectConstructionEC2Test(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.objectconstruction.ObjectConstructionChecker.class,
                "objectconstruction-cve",
                "-Anomsgtext",
                "-AuseValueChecker",
                "-nowarn");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"objectconstruction-cve"};
    }
}
