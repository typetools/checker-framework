package tests;

import java.io.File;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerFileTest;
import org.junit.runners.Parameterized.Parameters;

/** JUnit tests for the Nullness checker that issue javac errors. */
public class NullnessFbcJavacErrorsTest extends CheckerFrameworkPerFileTest {

    public NullnessFbcJavacErrorsTest(File testFile) {
        // TODO: remove soundArrayCreationNullness option once it's no
        // longer needed.  See issue #986:
        // https://github.com/typetools/checker-framework/issues/986
        super(
                testFile,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckPurityAnnotations",
                "-Anomsgtext",
                "-Xlint:deprecation",
                "-Alint=soundArrayCreationNullness,"
                        + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness-javac-errors"};
    }
}
