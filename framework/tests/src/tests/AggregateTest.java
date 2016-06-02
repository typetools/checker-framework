package tests;

import java.io.File;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.junit.runners.Parameterized.Parameters;

import tests.aggregate.AggregateOfCompoundChecker;

public class AggregateTest  extends CheckerFrameworkTest {

    public AggregateTest(File testFile) {
        super(testFile, AggregateOfCompoundChecker.class,"aggregate", "-Anomsgtext","-AresolveReflection");
    }


    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"aggregate"};
    }

}
