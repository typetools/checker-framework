package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Library for the Nullness checker when using safe defaults for unannotated source code. */
public class NullnessSafeDefaultsSourceCodeLibTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessSafeDefaultsSourceCodeLibTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AuseDefaultsForUncheckedCode=source,bytecode",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-safedefaultssourcecodelib"};
    }
}
