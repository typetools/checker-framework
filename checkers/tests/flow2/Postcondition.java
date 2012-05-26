import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;
import tests.util.*;

class Postcondition {
    
    String f1, f2, f3;
    Postcondition p;
    
    /***** normal postcondition ******/
    
    @EnsuresAnnotation(expression="f1", annotation=Odd.class)
    void oddF1() {
        f1 = null;
    }
    
    @EnsuresAnnotation(expression="p.f1", annotation=Odd.class)
    void oddF1_1() {
        p.f1 = null;
    }
    
    @EnsuresAnnotation(expression="#1.f1", annotation=Odd.class)
    void oddF1_2(final Postcondition param) {
        param.f1 = null;
    }
    
    @EnsuresAnnotation(expression="f1", annotation=Value.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void valueF1() {
    }
    
    @EnsuresAnnotation(expression="---", annotation=Value.class)
    //:: error: (flowexpr.parse.error)
    void error() {
    }
    
    @EnsuresAnnotation(expression="#1.#2", annotation=Value.class)
    //:: error: (flowexpr.parse.error)
    void error2(final String p1, final String p2) {
    }
    
    @EnsuresAnnotation(expression="f1", annotation=Value.class)
    void exception() {
        throw new RuntimeException();
    }
    
    @EnsuresAnnotation(expression="#1", annotation=Value.class)
    void param1(final @Value String f) {
    }
    @EnsuresAnnotation(expression={"#1","#2"}, annotation=Value.class)
    //:: error: (flowexpr.parameter.not.final)
    void param2(@Value String f, @Value String g) {
    }
    @EnsuresAnnotation(expression="#1", annotation=Value.class)
    //:: error: (flowexpr.parse.index.too.big)
    void param3() {
    }

    // basic postcondition test
    void t1(@Odd String p1, String p2) {
        valueF1();
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        oddF1();
        @Odd String l2 = f1;
        
        error();
    }
    
    // test parameter syntax
    void t2(@Odd String p1, String p2) {
        param3();
    }
    
    // postcondition with more complex flow expression
    void tn1(boolean b) {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p.f1;
        oddF1_1();
        @Odd String l2 = p.f1;
    }
    
    // postcondition with more complex flow expression
    void tn2(boolean b) {
        Postcondition param = null;
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = param.f1;
        oddF1_2(param);
        @Odd String l2 = param.f1;
    }
    
    /***** conditional postcondition ******/
    @EnsuresAnnotationIf(result=true, expression="f1", annotation=Odd.class)
    boolean condOddF1(boolean b) {
        if (b) {
            f1 = null;
            return true;
        }
        return false;
    }
    @EnsuresAnnotationIf(result=false, expression="f1", annotation=Odd.class)
    boolean condOddF1False(boolean b) {
        if (b) {
            return true;
        }
        f1 = null;
        return false;
    }
    @EnsuresAnnotationIf(result=false, expression="f1", annotation=Odd.class)
    boolean condOddF1Invalid(boolean b) {
        if (b) {
            f1 = null;
            return true;
        }
        //:: error: (contracts.conditional.postcondition.not.satisfied)
        return false;
    }
    @EnsuresAnnotationIf(result=false, expression="f1", annotation=Odd.class)
    //:: error: (contracts.conditional.postcondition.invalid.returntype)
    void wrongReturnType() {
    }
    @EnsuresAnnotationIf(result=false, expression="f1", annotation=Odd.class)
    //:: error: (contracts.conditional.postcondition.invalid.returntype)
    String wrongReturnType2() {
        f1 = null;
        return "";
    }
    
    // basic conditional postcondition test
    void t3(@Odd String p1, String p2) {
        condOddF1(true);
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (condOddF1(false)) {
            @Odd String l2 = f1;
        }
        //:: error: (assignment.type.incompatible)
        @Odd String l3 = f1;
    }
    
    // basic conditional postcondition test (inverted)
    void t4(@Odd String p1, String p2) {
        condOddF1False(true);
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        if (!condOddF1False(false)) {
            @Odd String l2 = f1;
        }
        //:: error: (assignment.type.incompatible)
        @Odd String l3 = f1;
    }
    
    // basic conditional postcondition test 2
    void t5(boolean b) {
        condOddF1(true);
        if (b) {
            //:: error: (assignment.type.incompatible)
            @Odd String l2 = f1;
        }
    }
}
