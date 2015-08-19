package tests;

import java.io.File;
import java.util.Collection;

import org.checkerframework.framework.test.DefaultCheckerTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests the constant value propagation type system.
 *
 * NOTE: $CHECKERFRAMEWORK/framework/tests/value/ needs to be on the classpath.
 * Otherwise ExceptionTest will fail because it cannot find the
 * ExceptionTest.class file for reflective method resolution.
 *
 * @author plvines
 *
 */
public class ValueTest extends DefaultCheckerTest {

    public ValueTest(File testFile) {
        super(testFile, org.checkerframework.common.value.ValueChecker.class,
                "value", "-Anomsgtext", "-Astubs=statically-executable.astub");
    }


    @Parameters
    public static Collection<Object[]> getTestFiles() {
        return TestUtilities.findNestedJavaTestFiles("value");
    }
}
