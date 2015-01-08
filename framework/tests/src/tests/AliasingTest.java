package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class AliasingTest extends ParameterizedCheckerTest {

    public AliasingTest(File testFile) {
        super(testFile,
                org.checkerframework.common.aliasing.AliasingChecker.class,
                "aliasing",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-Astubs=tests/aliasing/stubfile.astub");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("aliasing");
    }

}
