package tests.wpirunners;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.Assume;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import testlib.wholeprograminference.WholeProgramInferenceTestChecker;

/**
 * Tests whole-program type inference with the aid of .jaif files. This test is the second pass,
 * which ensures that with the annotations inserted, the errors are no longer issued.
 */
public class WholeProgramInferenceJaifsValidationTest extends FrameworkPerDirectoryTest {

    /** @param testFiles the files containing test code, which will be type-checked */
    public WholeProgramInferenceJaifsValidationTest(List<File> testFiles) {
        super(testFiles, WholeProgramInferenceTestChecker.class, "value", "-Anomsgtext");
    }

    @Override
    public void run() {
        // Only run if annotated files have been created.
        // See wholeProgramInferenceJaifTests task.
        if (new File("tests/whole-program-inference/annotated/").exists()) {
            super.run();
        }
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
