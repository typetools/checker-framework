package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class FenumSwingTest extends CheckerFrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public FenumSwingTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.fenum.FenumChecker.class,
                "fenum",
                "-Anomsgtext",
                "-Aquals=org.checkerframework.checker.fenum.qual.SwingVerticalOrientation,org.checkerframework.checker.fenum.qual.SwingHorizontalOrientation,org.checkerframework.checker.fenum.qual.SwingBoxOrientation,org.checkerframework.checker.fenum.qual.SwingCompassDirection,org.checkerframework.checker.fenum.qual.SwingElementOrientation,org.checkerframework.checker.fenum.qual.SwingTextOrientation");
        // TODO: check all qualifiers
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"fenumswing", "all-systems"};
    }
}
