package tests;

import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.basic.BasicChecker;
import checkers.util.test.Encrypted;

/**
 * Test suite for the basic checker, using a simple {@link Encrypted}
 * annotation.
 */
public class BasicEncryptedTest extends ParameterizedCheckerTest {

    public BasicEncryptedTest(String testName) {
        super(testName,
                BasicChecker.class.getName(),
                "basic",
                "-Anomsgtext",
                "-Aquals=checkers.util.test.Encrypted,checkers.util.test.PolyEncrypted");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("basic"); }

}
