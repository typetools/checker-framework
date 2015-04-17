package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.ParameterizedCheckerTest;
import org.junit.runners.Parameterized.Parameters;

import tests.aggregate.AggregateOfCompoundChecker;

public class AggregateTest  extends ParameterizedCheckerTest {

        public AggregateTest(File testFile) {
            super(testFile, AggregateOfCompoundChecker.class,"aggregate", "-Anomsgtext","-AresolveReflection");
        }

        @Parameters
        public static Collection<Object[]> data() {
            return testFiles("aggregate");
        }

    }


