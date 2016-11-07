package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker for issue #511. */
public class NullnessGenericWildcardTest extends CheckerFrameworkPerFileTest {

    public NullnessGenericWildcardTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                // This test reads bytecode .class files created by NullnessGenericWildcardLibTest
                "-cp",
                "dist/checker.jar:tests/build/testclasses/",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-genericwildcard"};
    }
}
