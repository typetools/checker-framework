package tests;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

public class I18nTest extends ParameterizedCheckerTest {

    public I18nTest(File testFile) {
        super(testFile, checkers.i18n.I18nChecker.class.getName(), "i18n",
                "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return testFiles("i18n", "all-systems"); }
}
