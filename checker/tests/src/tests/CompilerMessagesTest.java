package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Compiler Messages Checker. Depends on the compiler.properties file.
 */
public class CompilerMessagesTest extends CheckerFrameworkTest {

    public CompilerMessagesTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.compilermsgs.CompilerMessagesChecker.class,
                "compilermsg",
                "-Anomsgtext",
                "-Apropfiles=tests/compilermsg/compiler.properties");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"compilermsg", "all-systems"};
    }
}
