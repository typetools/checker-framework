import checkers.quals.EnsuresAnnotation;
import checkers.util.test.*;

import java.util.*;
import tests.util.*;
import dataflow.quals.*;

class MethodCallFlowExpr {
    
    @Pure
    String p1(int i) {
        return "";
    }
    
    @Pure
    String p1b(Long i) {
        return "";
    }
    
    @Pure
    String p1(String s) {
        return "";
    }
    
    String nonpure() {
        return "";
    }
    
    @Pure String p2(String s, long l, String s2) {
        return "";
    }
    
    @EnsuresAnnotation(expression="p1(1)", annotation=Odd.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void e1() {
        // don't bother with implementation
    }
    
    @EnsuresAnnotation(expression="p1(\"abc\")", annotation=Odd.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void e2() {
        // don't bother with implementation
    }
    
    @EnsuresAnnotation(expression="nonpure()", annotation=Odd.class)
    //:: error: (flowexpr.method.not.pure)
    void e3() {
    }
    
    @EnsuresAnnotation(expression="p2(\"abc\", 2L, p1(1))", annotation=Odd.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void e4() {
        // don't bother with implementation
    }
    
    @EnsuresAnnotation(expression="p1b(2L)", annotation=Odd.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void e5() {
        // don't bother with implementation
    }
    
    @EnsuresAnnotation(expression="p1b(null)", annotation=Odd.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void e6() {
        // don't bother with implementation
    }
    
    void t1(@Odd String p1, @Value String p2) {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p1(1);
        e1();
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p1(2);
        @Odd String l3 = p1(1);
    }
    
    void t2(@Odd String p1, @Value String p2) {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p1("abc");
        e2();
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p1("def");
        //:: error: (assignment.type.incompatible)
        @Odd String l2b = p1(1);
        @Odd String l3 = p1("abc");
    }
    
    void t3() {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p2("abc", 2L, p1(1));
        e4();
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p2("abc", 2L, p1(2));
        //:: error: (assignment.type.incompatible)
        @Odd String l2b = p2("abc", 1L, p1(1));;
        @Odd String l3 = p2("abc", 2L, p1(1));
    }
    
    void t4(@Odd String p1, @Value String p2) {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p1b(2L);
        e5();
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p1b(null);
        //:: error: (assignment.type.incompatible)
        @Odd String l2b = p1b(1L);
        @Odd String l3 = p1b(2L);
    }
    
    void t5(@Odd String p1, @Value String p2) {
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = p1b(null);
        e6();
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p1b(1L);
        @Odd String l3 = p1b(null);
    }
}
