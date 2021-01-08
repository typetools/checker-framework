import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class MetaPostcondition {

    String f1, f2, f3;
    MetaPostcondition p;

    /** *** normal postcondition ***** */
    @EnsuresOdd("f1")
    void oddF1() {
        f1 = null;
    }

    @EnsuresOdd("p.f1")
    void oddF1_1() {
        p.f1 = null;
    }

    @EnsuresOdd("#1.f1")
    void oddF1_2(final MetaPostcondition param) {
        param.f1 = null;
    }

    @EnsuresOdd("f1")
    // :: error: (contracts.postcondition.not.satisfied)
    void oddF1_error() {}

    @EnsuresOdd("---")
    // :: error: (flowexpr.parse.error)
    void error() {}

    @EnsuresOdd("#1.#2")
    // :: error: (flowexpr.parse.error)
    void error2(final String p1, final String p2) {}

    @EnsuresOdd("f1")
    void exception() {
        throw new RuntimeException();
    }

    @EnsuresOdd("#1")
    void param1(final @Odd String f) {}

    @EnsuresOdd({"#1", "#2"})
    // :: error: (flowexpr.parameter.not.final)
    void param2(@Odd String f, @Odd String g) {
        f = g;
    }

    @EnsuresOdd("#1")
    // :: error: (flowexpr.parse.index.too.big)
    void param3() {}

    // basic postcondition test
    void t1(@Odd String p1, String p2) {
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        oddF1();
        @Odd String l2 = f1;

        // :: error: (flowexpr.parse.error.postcondition)
        error();
    }

    // test parameter syntax
    void t2(@Odd String p1, String p2) {
        // :: error: (flowexpr.parse.index.too.big)
        param3();
    }

    // postcondition with more complex expression
    void tn1(boolean b) {
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = p.f1;
        oddF1_1();
        @Odd String l2 = p.f1;
    }

    // postcondition with more complex expression
    void tn2(boolean b) {
        MetaPostcondition param = null;
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = param.f1;
        oddF1_2(param);
        @Odd String l2 = param.f1;
    }

    // basic postcondition test
    void tnm1(@Odd String p1, @Value String p2) {
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        // :: error: (assignment.type.incompatible)
        @Value String l2 = f2;

        // :: error: (flowexpr.parse.error.postcondition)
        error2(p1, p2);
    }

    /** conditional postcondition */
    @EnsuresOddIf(result = true, expression = "f1")
    boolean condOddF1(boolean b) {
        if (b) {
            f1 = null;
            return true;
        }
        return false;
    }

    @EnsuresOddIf(result = false, expression = "f1")
    boolean condOddF1False(boolean b) {
        if (b) {
            return true;
        }
        f1 = null;
        return false;
    }

    @EnsuresOddIf(result = false, expression = "f1")
    boolean condOddF1Invalid(boolean b) {
        if (b) {
            f1 = null;
            return true;
        }
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return false;
    }

    @EnsuresOddIf(result = false, expression = "f1")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    void wrongReturnType() {}

    @EnsuresOddIf(result = false, expression = "f1")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    String wrongReturnType2() {
        f1 = null;
        return "";
    }

    // basic conditional postcondition test
    void t3(@Odd String p1, String p2) {
        condOddF1(true);
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (condOddF1(false)) {
            @Odd String l2 = f1;
        }
        // :: error: (assignment.type.incompatible)
        @Odd String l3 = f1;
    }

    // basic conditional postcondition test (inverted)
    void t4(@Odd String p1, String p2) {
        condOddF1False(true);
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (!condOddF1False(false)) {
            @Odd String l2 = f1;
        }
        // :: error: (assignment.type.incompatible)
        @Odd String l3 = f1;
    }

    // basic conditional postcondition test 2
    void t5(boolean b) {
        condOddF1(true);
        if (b) {
            // :: error: (assignment.type.incompatible)
            @Odd String l2 = f1;
        }
    }
}
