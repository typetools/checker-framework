package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.checkerframework.framework.test.CheckerFrameworkTest;
import org.checkerframework.framework.test.TestUtilities;
import org.junit.runners.Parameterized.Parameters;

/**
 * JUnit tests for the Interning Checker, which tests the Interned annotation.
 */
public class OIGJTest extends CheckerFrameworkTest {

    public OIGJTest(File testFile) {
        super(testFile,
                org.checkerframework.checker.oigj.OIGJChecker.class,
                "oigj",
                "-Anomsgtext");
    }

    @Parameters
    public static List<File> getTestFiles() {
        return filter(TestUtilities.findNestedJavaTestFiles("oigj", "all-systems"));
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

    protected static boolean filter(Object o) {
        // TODO: Default qualifiers for this file seem wrong.
        return o.toString().equals("tests/all-systems/GenericsBounds.java") ||
                o.toString().equals("tests/all-systems/MethodTypeVars.java") ||
                o.toString().equals("tests/all-systems/Ternary.java") ||
                o.toString().equals("tests/all-systems/FieldWithInit.java") ||
                o.toString().equals("tests/all-systems/TypeVars.java") ||
                o.toString().equals("tests/all-systems/RawTypes.java") ||
                o.toString().equals("tests/all-systems/RawTypeAssignment.java") ||
                o.toString().equals("tests/all-systems/GenericsCasts.java") ||
                o.toString().equals("tests/all-systems/GenericsEnclosing.java") ||
                o.toString().equals("tests/all-systems/GenericTest12.java") ||
                o.toString().equals("tests/all-systems/Options.java") ||
                o.toString().equals("tests/all-systems/ForEach.java") ||
                o.toString().equals("tests/all-systems/Arrays.java") ||
                o.toString().equals("tests/all-systems/GenericTest11full.java") ||
                o.toString().equals("tests/all-systems/MissingBoundAnnotations.java") ||
                o.toString().equals("tests/all-systems/Enums.java");
    }
}
