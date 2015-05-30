package tests;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class TaintingClassicTest extends ParameterizedCheckerTest {

    public TaintingClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.tainting.classic.TaintingClassicChecker.class,
                "tainting_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("tainting", "all-systems");
    }
}
