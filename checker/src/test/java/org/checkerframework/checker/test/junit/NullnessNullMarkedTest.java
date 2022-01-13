package org.checkerframework.checker.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.Collections;
import java.util.List;

/** JUnit tests for the Nullness checker. */
public class NullnessNullMarkedTest extends CheckerFrameworkPerDirectoryTest {

    /**
     * Create a NullnessNullMarkedTest.
     *
     * @param testFiles the files containing test code, which will be type-checked
     */
    public NullnessNullMarkedTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                Collections.singletonList("../../jspecify/build/libs/jspecify-0.0.0-SNAPSHOT.jar"),
                "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-nullmarked"};
    }

    @Override
    @Test
    public void run() {
        /*
         * Skip under JDK8: checker/bin-devel/build.sh doesn't build JSpecify under that version
         * (since the JSpecify build requires JDK9+), so there would be no JSpecify jar, and tests
         * would fail on account of the missing classes.
         */
        if (TestUtilities.IS_AT_LEAST_9_JVM) {
            super.run();
        }
    }
}
