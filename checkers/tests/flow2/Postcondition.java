import checkers.util.test.*;

import java.util.*;
import checkers.quals.*;

class Postcondition {
    
    String f1, f2, f3;
    
    @EnsuresAnnotation(expression="f1", annotation=Odd.class)
    void oddF1() {
    }
    
    @EnsuresAnnotation(expression="f1", annotation=Value.class)
    void valueF1() {
    }

    // basic postcondition test
    void t1(@Odd String p1, String p2) {
        valueF1();
        //:: error: (assignment.type.incompatible)
        @Odd String l1 = f1;
        oddF1();
        @Odd String l2 = f1;
    }
    
}
