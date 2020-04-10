package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * tests the returns receiver checker's lombok integration, please not that the test files have been
 * delomobok'd
 */
public class ReturnsReceiverLombokTest extends CheckerFrameworkPerDirectoryTest {
    public ReturnsReceiverLombokTest(List<File> testFiles) {
        super(
                testFiles,
                ReturnsReceiverChecker.class,
                "returnsreceiverlombok",
                "-Anomsgtext",
                "-nowarn",
                "-AsuppressWarnings=type.anno.before.modifier");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiverlombok"};
    }
}
