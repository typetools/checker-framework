package tests.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import testlib.wholeprograminference.WholeProgramInferenceTestChecker;

/**
 * Tests whole-program type inference with stub files. This test is the second pass, which ensures
 * that with the stubs in place, the errors that those annotations remove are no longer issued.
 */
public class WholeProgramInferenceStubsValidationTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceStubsValidationTest(List<File> testFiles) {
        super(
                testFiles,
                WholeProgramInferenceTestChecker.class,
                "whole-program-inference/annotated",
                "-Anomsgtext",
                "-Astubs=build/whole-program-inference",
                // "-AstubDebug",
                "-AmergeStubsWithSource");
    }

    @Before
    public void before() throws Exception {
        Assume.assumeTrue("running_wpi_tests".equals(System.getProperty("wpiTestStatus")));
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"whole-program-inference/annotated/"};
    }
}
