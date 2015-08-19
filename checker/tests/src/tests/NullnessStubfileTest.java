package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class NullnessStubfileTest extends DefaultCheckerTest {

    public NullnessStubfileTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-Anomsgtext",
                "-AprintErrorStack",
                "-AstubWarnIfNotFound",
                "-Astubs="
                        + "tests/nullness-stubfile/stubfile1.astub:"
                        + "tests/nullness-stubfile/stubfile2.astub");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("nullness-stubfile");
    }

}
