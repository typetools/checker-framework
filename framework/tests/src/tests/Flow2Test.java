package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.util.FlowTestChecker;

/**
 * Tests for the flow-sensitive part of the framework. These tests complement the tests of {@link
 * FlowTest} and have been written when the org.checkerframework.dataflow analysis has been
 * completely rewritten.
 *
 * @author Stefan Heule
 */
public class Flow2Test extends CheckerFrameworkPerDirectoryTest {

    public Flow2Test(List<File> testFiles) {
        super(testFiles, FlowTestChecker.class, "flow", "-Anomsgtext", "-AcheckPurityAnnotations");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flow2"};
    }
}
