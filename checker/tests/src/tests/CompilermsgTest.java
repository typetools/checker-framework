package tests;

import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

/**
 * JUnit tests for the Compiler Message checker. Depends on the compiler.properties file.
 */
public class CompilermsgTest extends ParameterizedCheckerTest {

    public CompilermsgTest(File testFile) {
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
