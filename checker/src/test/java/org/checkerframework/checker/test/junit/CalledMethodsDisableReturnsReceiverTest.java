package org.checkerframework.checker.test.junit;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** Basic tests for the Called Methods Checker. */
public class CalledMethodsDisableReturnsReceiverTest extends CheckerFrameworkPerDirectoryTest {
    public CalledMethodsDisableReturnsReceiverTest(List<File> testFiles) {
        super(
                testFiles,
                CalledMethodsChecker.class,
                "calledmethods-disablereturnsreceiver",
                "-Anomsgtext",
                "-AdisableReturnsReceiver",
                "-encoding",
                "UTF-8");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"calledmethods-disablereturnsreceiver"};
    }
}
