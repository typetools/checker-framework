package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 */
public class PolyAllTest extends DefaultCheckerTest {

    public PolyAllTest(File testFile) {
        super(testFile,
                polyall.PolyAllChecker.class,
                "polyall",
                "-Anomsgtext",
                "-Astubs=tests/polyall/polyall.astub");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("polyall");
    }
}