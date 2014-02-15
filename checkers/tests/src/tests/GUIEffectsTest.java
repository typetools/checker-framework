package tests;

import checkers.util.test.ParameterizedCheckerTest;

import java.io.File;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class GUIEffectsTest extends ParameterizedCheckerTest {

    public GUIEffectsTest(File testFile) {
        super(testFile, checkers.guieffects.GUIEffectsChecker.class, "guieffects",
                "-Anomsgtext");
                //, "-Alint=debugSpew");
    }

    @Parameters
    public static Collection<Object[]> data() {
	return testFiles("guieffects", "all-systems");
    }
}
