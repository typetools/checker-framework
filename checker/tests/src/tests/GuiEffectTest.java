package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;

public class GuiEffectTest extends CheckerFrameworkPerDirectoryTest {

    public GuiEffectTest(List<File> testFiles) {
        super(
                testFiles,
                org.checkerframework.checker.guieffect.GuiEffectChecker.class,
                "guieffect",
                "-Anomsgtext");
        // , "-Alint=debugSpew");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"guieffect", "all-systems"};
    }
}
