package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** JUnit tests for the Nullness checker when running with concurrent semantics. */
public class NullnessConcurrentTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessConcurrentTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessConcurrentTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AconcurrentSemantics",
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-concurrent-semantics"};
    }
}
