package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker when using safe defaults for unannotated source code. */
public class NullnessSafeDefaultsSourceCodeTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessSafeDefaultsSourceCodeTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseDefaultsForUncheckedCode=source",
                // This test reads bytecode .class files created by
                // NullnessSafeDefaultsSourceCodeLibTest
                "-cp",
                "dist/checker.jar:tests/build/testclasses/",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-safedefaultssourcecode"};
    }
}
