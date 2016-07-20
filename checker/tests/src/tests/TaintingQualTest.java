package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class TaintingQualTest extends CheckerFrameworkPerDirectoryTest {

    public TaintingQualTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.experimental.tainting_qual.TaintingCheckerAdapter
                        .class,
                "tainting_qual",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"tainting_qual", "all-systems"};
    }
}
