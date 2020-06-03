package tests.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameters;
import testlib.wholeprograminference.WholeProgramInferenceTestChecker;

/**
 * Tests whole-program inference with the aid of stub files. This test is the first pass on the test
 * data, which generates the stubs.
 *
 * <p>IMPORTANT: The errors captured in the tests located in tests/whole-program-inference/ are not
 * relevant. The meaning of this test class is to test if the generated stub files are similar to
 * the expected ones. The errors on .java files must be ignored.
 */
@Category(WholeProgramInferenceStubsTest.class)
public class WholeProgramInferenceStubsTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceStubsTest(List<File> testFiles) {
        super(
                testFiles,
                WholeProgramInferenceTestChecker.class,
                "whole-program-inference/non-annotated",
                "-Anomsgtext",
                "-Ainfer=stubs");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"whole-program-inference/non-annotated"};
    }
}
