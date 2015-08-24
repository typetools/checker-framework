package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

import tests.aggregate.AggregateOfCompoundChecker;

public class AggregateTest  extends DefaultCheckerTest {

    public AggregateTest(File testFile) {
        super(testFile, AggregateOfCompoundChecker.class,"aggregate", "-Anomsgtext","-AresolveReflection");
    }


    @Parameters
    public static String[] getTestDirs() {
        return new String[]{"aggregate"};
    }

}


