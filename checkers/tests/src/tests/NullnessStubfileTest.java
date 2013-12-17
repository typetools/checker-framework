package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class NullnessStubfileTest extends ParameterizedCheckerTest {

    public NullnessStubfileTest(File testFile) {
        super(
                testFile,
                checkers.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-Astubs="
                        + "tests/nullness-stubfile/stubfile1.astub:"
                        + "tests/nullness-stubfile/stubfile2.astub");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("nullness-stubfile");
    }

}
