package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

public class GuiEffectTest extends ParameterizedCheckerTest {

    public GuiEffectTest(File testFile) {
        super(testFile, org.checkerframework.checker.guieffect.GuiEffectChecker.class, "guieffect",
                "-Anomsgtext");
                //, "-Alint=debugSpew");
    }

    @Parameters
    public static Collection<Object[]> data() {
	return testFiles("guieffect", "all-systems");
    }
}
