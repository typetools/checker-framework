package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the compound checker design pattern
 *
 * @author smillst
 *
 */
public class CompoundCheckerTest extends DefaultCheckerTest {

    public CompoundCheckerTest(File testFile) {
        super(testFile, tests.compound.CompoundChecker.class,
                "compound-checker", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("compound-checker");
    }
}
