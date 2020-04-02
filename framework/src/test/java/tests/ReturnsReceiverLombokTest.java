package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test runner for tests of the Returns Receiver Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/lombok} folder. To add a new test case, create
 * a Java file in that directory. The file contains "// ::" comments to indicate expected errors and
 * warnings; see https://github.com/typetools/checker-framework/blob/master/checker/tests/README .
 */
public class ReturnsReceiverLombokTest extends CheckerFrameworkPerDirectoryTest {
    public ReturnsReceiverLombokTest(List<File> testFiles) {
        super(
                testFiles,
                ReturnsReceiverChecker.class,
                "lombok",
                "-Anomsgtext",
                "-nowarn",
                "-AsuppressWarnings=type.anno.before.modifier");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiverlombok"};
    }
}
