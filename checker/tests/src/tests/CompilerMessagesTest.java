package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

/**
 * JUnit tests for the Compiler Messages Checker. Depends on the compiler.properties file.
 */
public class CompilerMessagesTest extends ParameterizedCheckerTest {

    public CompilerMessagesTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.compilermsgs.CompilerMessagesChecker.class,
                "compilermsg", "-Anomsgtext",
                "-Apropfiles=tests/compilermsg/compiler.properties");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("compilermsg", "all-systems");
    }

}
