package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class GUIEffectsTest extends ParameterizedCheckerTest {

    public GUIEffectsTest(File testFile) {
        super(testFile, org.checkerframework.checker.guieffects.GUIEffectsChecker.class, "guieffects",
                "-Anomsgtext");
                //, "-Alint=debugSpew");
    }

    @Parameters
    public static Collection<Object[]> data() {
	return testFiles("guieffects", "all-systems");
    }
}
