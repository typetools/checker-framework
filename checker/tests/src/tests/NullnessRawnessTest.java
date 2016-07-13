package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.checker.nullness.AbstractNullnessChecker;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Nullness checker (that uses the rawness type system for
 * initialization).
 */
public class NullnessRawnessTest extends CheckerFrameworkPerDirectoryTest {

    public NullnessRawnessTest(List<File> testFiles) {
        // TODO: remove forbidnonnullarraycomponents option once it's no
        // longer needed.  See issues 154, 322, and 433.
        super(
                testFiles,
                org.checkerframework.checker.nullness.NullnessRawnessChecker.class,
                "nullness",
                "-AcheckPurityAnnotations",
                "-Anomsgtext",
                "-Xlint:deprecation",
                "-Alint=forbidnonnullarraycomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"nullness", "all-systems"};
    }
}
