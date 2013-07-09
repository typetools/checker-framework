package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import tests.util.Encrypted;
import checkers.subtyping.SubtypingChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * Test suite for the Basic Checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicEncryptedTest extends ParameterizedCheckerTest {

    public BasicEncryptedTest(File testFile) {
        super(testFile,
                SubtypingChecker.class.getName(),
                "basic",
                "-Anomsgtext",
                "-Aquals=tests.util.Encrypted,tests.util.PolyEncrypted,checkers.quals.Unqualified");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("basic", "all-systems"); }

}
