import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;

class Postcondition {
    
    String f1, f2, f3;
    Postcondition p;
    
    @EnsuresAnnotation(expression="f1", annotation=Odd.class)
    void oddF1() {
    }
    
    @EnsuresAnnotation(expression="f1", annotation=Value.class)
    void valueF1() {
    }
    
    @EnsuresAnnotation(expression="---", annotation=Value.class)
    //:: error: (flowexpr.parse.error)
    void error() {
    }
    
    @EnsuresAnnotation(expression="#1", annotation=Value.class)
    void param1(String f) {
    }
    @EnsuresAnnotation(expression={"#1","#2"}, annotation=Value.class)
    void param2(String f, String g) {
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
        //:: error: (assignment.type.incompatible)
        @Value String l1 = f1;
        param1(f1);
        @Value String l2 = f1;
        
        //:: error: (assignment.type.incompatible)
        @Value String l3 = f2;
        //:: error: (assignment.type.incompatible)
        @Value String l4 = f3;
        param2(f2, f3);
        @Value String l5 = f2;
        @Value String l6 = f3;
        
        param3();
    }
}
