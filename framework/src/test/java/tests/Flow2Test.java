package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.util.FlowTestChecker;

/**
 * Tests for the flow-sensitive part of the framework. These tests complement the tests of {@link
 * FlowTest} and have been written when the org.checkerframework.dataflow analysis has been
 * completely rewritten.
 */
public class Flow2Test extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public Flow2Test(List<File> testFiles) {
        super(testFiles, FlowTestChecker.class, "flow", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flow2"};
    }
}
