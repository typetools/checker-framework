package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingEncryptedTest extends DefaultCheckerTest {

    public SubtypingEncryptedTest(File testFile) {
        super(testFile,
                org.checkerframework.common.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=tests.util.Encrypted,tests.util.PolyEncrypted,org.checkerframework.framework.qual.Unqualified");
    }

    @Parameters
    public static String [] getTestDirs() {
        return new String[]{"subtyping", "all-systems"};
    }

}
