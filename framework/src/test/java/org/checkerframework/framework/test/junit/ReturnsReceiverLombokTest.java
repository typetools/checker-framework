package org.checkerframework.framework.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the returns receiver checker's lombok integration, the test files in
 * tests/returnsreceiverlombok package will be delomboked into tests/returnsreceiverdelomboked
 * package before running the test and the returns receiver checker will run on the generated codes.
 */
public class ReturnsReceiverLombokTest extends CheckerFrameworkPerDirectoryTest {
    public ReturnsReceiverLombokTest(List<File> testFiles) {
        super(
                testFiles,
                ReturnsReceiverChecker.class,
                "returnsreceiverdelomboked",
                "-Anomsgtext",
                "-nowarn",
                "-AsuppressWarnings=type.anno.before.modifier");
    }

    @Override
    public void run() {
        // Only run if delomboked codes have been created.
        if (!new File("tests/returnsreceiverdelomboked/").exists()) {
            throw new RuntimeException("delombok task must be run before this test.");
        }
        super.run();
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"returnsreceiverdelomboked"};
    }
}
