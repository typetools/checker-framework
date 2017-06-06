package tests;

import java.io.File;
import java.util.List;
import org.checkerframework.framework.test.CheckerFrameworkPerDirectoryTest;
import org.junit.runners.Parameterized.Parameters;
import testlib.aggregate.AggregateOfCompoundChecker;

public class AggregateTest extends CheckerFrameworkPerDirectoryTest {

    public AggregateTest(List<File> testFiles) {
        super(
                testFiles,
                AggregateOfCompoundChecker.class,
                "aggregate",
                "-Anomsgtext",
                "-AresolveReflection");
    }

    @Parameters
    public static String[] getTestDirs() {
        return new String[] {"aggregate"};
    }
}
