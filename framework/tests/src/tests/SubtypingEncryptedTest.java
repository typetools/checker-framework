package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingEncryptedTest extends CheckerFrameworkTest {

    public SubtypingEncryptedTest(File testFile) {
        super(
                testFile,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=tests.util.Encrypted,tests.util.PolyEncrypted,org.checkerframework.framework.qual.Unqualified");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"subtyping", "all-systems"};
    }
}
