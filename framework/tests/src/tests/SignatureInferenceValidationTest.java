package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests signature type inference with the aid of .jaif files.
 *
 * IMPORTANT: The errors captured in the tests located in tests/signature-inference/
 * are not relevant. The meaning of this test class is to test if the generated
 * .jaif files are similar to the expected ones. The errors on .java files
 * must be ignored.
 *
 * @author pbsf
 */
public class SignatureInferenceValidationTest extends CheckerFrameworkTest {


    public SignatureInferenceValidationTest(File testFile) {
        super(testFile, tests.signatureinference.SignatureInferenceTestChecker.class,
                "value", "-Anomsgtext");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"signature-inference/annotated/"};
    }

}
