package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import tests.util.Encrypted;

/**
 * Test suite for the Basic Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicEncryptedTest extends ParameterizedCheckerTest {

    public BasicEncryptedTest(File testFile) {
        super(testFile,
                checkers.subtyping.SubtypingChecker.class,
                "basic",
                "-Anomsgtext",
                "-Aquals=tests.util.Encrypted,tests.util.PolyEncrypted,checkers.quals.Unqualified");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("basic", "all-systems");
    }

}
