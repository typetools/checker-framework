package org.checkerframework.framework.test.junit;

import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.checkerframework.framework.testchecker.util.FlowTestChecker;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/** Tests for the {@code -AsuggestPureMethods} command-line argument. */
public class PuritySuggestionsTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public PuritySuggestionsTest(List<File> testFiles) {
        super(
                testFiles,
                FlowTestChecker.class,
                "flow",
                "-Anomsgtext",
                "-AsuggestPureMethods",
                "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"purity-suggestions"};
    }
}
