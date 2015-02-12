package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the compound checker design pattern
 *
 * @author smillst
 *
 */
public class CompoundCheckerTest extends ParameterizedCheckerTest {

    public CompoundCheckerTest(File testFile) {
        super(testFile, tests.compound.CompoundChecker.class,
                "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() {
        return testFiles("compound-checker");
    }
}
