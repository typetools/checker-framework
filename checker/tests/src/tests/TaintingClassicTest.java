package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collection;

public class TaintingClassicTest extends DefaultCheckerTest {

    public TaintingClassicTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.tainting.classic.TaintingClassicChecker.class,
                "tainting_classic",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("tainting", "all-systems");
    }
}
