package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Library for the Nullness checker for issue #511. */
public class NullnessGenericWildcardLibTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessGenericWildcardLibTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-genericwildcardlib"};
    }
}
