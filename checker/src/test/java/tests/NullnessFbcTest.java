package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker (that uses the Freedom Before Commitment type system for
 * initialization).
 */
public class NullnessFbcTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public NullnessFbcTest(List<File> testFiles) {
        // TODO: remove forbidnonnullarraycomponents option once it's no
        // longer needed.  See issue #986:
        // https://github.com/typetools/checker-framework/issues/986
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessChecker.class,
                "nullness",
                "-AcheckPurityAnnotations",
                "-Anomsgtext",
                "-Xlint:deprecation",
                "-Alint=forbidnonnullarraycomponents,"
                        + NullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness", "initialization/fbc", "all-systems"};
    }
}
