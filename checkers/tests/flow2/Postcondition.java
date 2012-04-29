import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;

class Postcondition {
    
    String f1, f2, f3;
    Postcondition p;
    
    @EnsuresAnnotation(expression="f1", annotation=Odd.class)
    void oddF1() {
        f1 = null;
    }
    
    @EnsuresAnnotation(expression="f1", annotation=Value.class)
    //:: error: (contracts.postcondition.not.satisfied)
    void valueF1() {
    }
    
    @EnsuresAnnotation(expression="---", annotation=Value.class)
    //:: error: (flowexpr.parse.error)
    void error() {
    }
    
    @EnsuresAnnotation(expression="#1", annotation=Value.class)
    void param1(@Value String f) {
    }
    @EnsuresAnnotation(expression={"#1","#2"}, annotation=Value.class)
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
}
