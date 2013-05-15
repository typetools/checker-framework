package tests;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import org.junit.runners.Parameterized.Parameters;

import checkers.nullness.AbstractNullnessChecker;
import checkers.util.test.ParameterizedCheckerTest;

/**
 * JUnit tests for the Nullness checker (that uses the Freedom Before Commitment
 * type system for initialization).
 */
public class NullnessFbcTest extends ParameterizedCheckerTest {

    public NullnessFbcTest(File testFile) {
        // TODO: remove arrays:forbidnonnullcomponents option once it's no
        // longer needed.
        super(testFile, checkers.nullness.NullnessChecker.class.getName(),
                "nullness", "-Anomsgtext", "-Xlint:deprecation",
                "-Alint=arrays:forbidnonnullcomponents,"
                        + AbstractNullnessChecker.LINT_REDUNDANTNULLCOMPARISON);
    }

    @Parameters
    public static Collection<Object[]> data() {
        return filter(testFiles("nullness", "initialization/fbc", "all-systems"));
    }

    // Duplicate from JavariTest.
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
        // Nullness type systems don't type null as bottom
        return o.toString().equals("tests/all-systems/GenericNull.java");
    }

}
