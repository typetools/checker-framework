package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runners.Parameterized.Parameters;

import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Javari annotation checker.
 */
public class JavariTest extends ParameterizedCheckerTest {

    public JavariTest(File testFile) {
        super(testFile, checkers.javari.JavariChecker.class.getCanonicalName(),
                "javari", "-Anomsgtext");
    }

    @Parameters
    public static Collection<Object[]> data() { return filter(testFiles("javari", "all-systems")); }

    // TODO: I want this method somewhere in ParameterizedChecker, but as
    // all these methods are static, I didn't find a fast way :-(
    // Duplicated in OIGJTest!
    protected static Collection<Object[]> filter(Collection<Object[]> in) {
        Collection<Object[]> out = new ArrayList<Object[]>();
        for (Object[] oa : in) {
            Collection<Object> oout = new LinkedList<Object>();
            for (Object o : oa) {
                if (!filter(o)) {
                    oout.add(o);
                }
            }
            if (!oout.isEmpty()) {
                out.add(oout.toArray());
            }
        }
        return out;
    }

    protected static boolean filter(Object o) {
        // One part of this test case doesn't work with Javari, because
        // a the upper bound of a type variable get's defaulted to @Readonly.
        // TODO: split up the test case in smaller parts.
        return o.toString().equals("tests/all-systems/GenericsCasts.java") ||
                o.toString().equals("tests/all-systems/Ternary.java") ||
                o.toString().equals("tests/all-systems/Enums.java") ||
                o.toString().equals("tests/all-systems/TypeVars.java") ||
                o.toString().equals("tests/all-systems/RawTypes.java") ||
                o.toString().equals("tests/all-systems/ForEach.java") ||
                o.toString().equals("tests/all-systems/MethodTypeVars.java");
    }
}
