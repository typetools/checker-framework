package tests;

import java.io.File;
import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

public class GuiEffectTest extends CheckerFrameworkTest {

    public GuiEffectTest(File testFile) {
        super(
                testFile,
                org.checkerframework.checker.guieffect.GuiEffectChecker.class,
                "guieffect",
                "-Anomsgtext");
        //, "-Alint=debugSpew");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"guieffect", "all-systems"};
    }
}
