package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

/**
 * Test suite for the Subtyping Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class SubtypingEncryptedTest extends ParameterizedCheckerTest {

    public SubtypingEncryptedTest(File testFile) {
        super(testFile,
                checkers.subtyping.SubtypingChecker.class,
                "subtyping",
                "-Anomsgtext",
                "-Aquals=tests.util.Encrypted,tests.util.PolyEncrypted,checkers.quals.Unqualified");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("subtyping", "all-systems");
    }

}
