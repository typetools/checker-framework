package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker when array subtyping is invariant. */
public class NullnessInvariantArraysTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessInvariantArraysTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AinvariantArrays",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-invariantarrays"};
    }
}
