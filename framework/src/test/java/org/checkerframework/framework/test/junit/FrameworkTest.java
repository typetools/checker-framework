package org.checkerframework.framework.test.junit;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.checkerframework.framework.testchecker.util.EvenOddChecker;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Checker Framework, using the {@link EvenOddChecker}. */
public class FrameworkTest extends CheckerFrameworkPerFileTest {

    public FrameworkTest(File testFile) {
        super(testFile, EvenOddChecker.class, "framework", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"framework", "all-systems"};
    }
}
