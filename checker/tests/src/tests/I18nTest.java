package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

public class I18nTest extends DefaultCheckerTest {

    public I18nTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.i18n.I18nChecker.class,
                "i18n",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("i18n", "all-systems");
    }
}
