package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.FrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.flowexpression.FlowExpressionChecker;

public class FlowExpressionCheckerTest extends FrameworkPerDirectoryTest {

    public FlowExpressionCheckerTest(List<File> testFiles) {
        super(testFiles, FlowExpressionChecker.class, "flowexpression", "-Anomsgtext");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"flowexpression", "all-systems"};
    }
}
