package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Lock checker when using safe defaults for unchecked source code. */
public class LockSafeDefaultsTest extends CheckerFrameworkPerDirectoryTest {

    public LockSafeDefaultsTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.lock.LockChecker.class,
                "lock",
                "-AuseDefaultsForUncheckedCode=source",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"lock-safedefaults"};
    }
}
