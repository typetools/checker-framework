package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized;

public class CalledMethodsEC2Test extends CheckerFrameworkPerDirectoryTest {
    public CalledMethodsEC2Test(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-cve",
                "-Anomsgtext",
                "-AcalledMethodsUseValueChecker",
                "-nowarn");
    }

    @Parameterized.Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-cve"};
    }
}
