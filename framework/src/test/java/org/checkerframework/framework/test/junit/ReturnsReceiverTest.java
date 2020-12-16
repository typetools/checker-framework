package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test runner for tests of the Returns Receiver Checker.
 *
 * <p>Tests appear as Java files in the {@code tests/returnsreceiver} folder. To add a new test
 * case, create a Java file in that directory.
 */
public class ReturnsReceiverTest extends CheckerFrameworkPerDirectoryTest {
    public ReturnsReceiverTest(List<File> testFiles) {
        super(
                testFiles,
                ReturnsReceiverChecker.class,
                "returnsreceiver",
                "-Anomsgtext",
                "-Astubs=stubs/",
                "-nowarn");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiver", "all-systems"};
    }
}
