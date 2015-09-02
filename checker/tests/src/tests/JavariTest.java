package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends CheckerFrameworkTest {

    public JavariTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.javari.JavariChecker.class,
                "javari",
                "-Anomsgtext");
    }

    @Parameters
    public static List<File> getTestFiles() {
        return filter(TestUtilities.findNestedJavaTestFiles("javari", "all-systems"));
    }

    // TODO: I want this method somewhere in ParameterizedChecker, but as
    // all these methods are static, I didn't find a fast way :-(
    // Duplicated in OIGJTest!
    protected static List<File> filter(List<File> in) {
        List<File> out = new ArrayList<File>();
        for (File file : in) {
            if (!filter(file)) {
                out.add(file);
            }
        }
        return out;
    }

    protected static boolean filter(File o) {
        // One part of this test case doesn't work with Javari, because
        // a the upper bound of a type variable get's defaulted to @Readonly.
        // TODO: split up the test case in smaller parts.
        return o.toString().equals("tests/all-systems/GenericsCasts.java") ||
                o.toString().equals("tests/all-systems/Ternary.java") ||
                o.toString().equals("tests/all-systems/Enums.java") ||
                o.toString().equals("tests/all-systems/TypeVars.java") ||
                o.toString().equals("tests/all-systems/RawTypes.java") ||
                o.toString().equals("tests/all-systems/ForEach.java") ||
                o.toString().equals("tests/all-systems/WildcardSuper.java") ||
                o.toString().equals("tests/all-systems/GenericTest11full.java") ||
                o.toString().equals("tests/all-systems/MethodTypeVars.java");
    }
}
