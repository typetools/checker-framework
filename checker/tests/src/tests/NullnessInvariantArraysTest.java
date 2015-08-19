package tests;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker when array subtyping is invariant.
 */
public class NullnessInvariantArraysTest extends DefaultCheckerTest {

    public NullnessInvariantArraysTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AinvariantArrays",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("nullness-invariantarrays");
    }

}
