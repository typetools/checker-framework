import checkers.util.test.*;
import java.util.*;

class Basic {

    // basic tests to make sure everything works
    void t1(@Odd String p1, String p2) {
        String l1 = p1;
        //:: error: (assignment.type.incompatible)
        @Odd String l2 = p2;
    }
    
    // basic local variable flow sensitivity
    void t2(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
        }
        //:: error: (assignment.type.incompatible)
        @Odd String l3 = l1;
        
        l1 = p1;
        while (b1) {
            l1 = p2;
        }
        //:: error: (assignment.type.incompatible)
        @Odd String l4 = l1;
    }
    
    void t2b(@Odd String p1, String p2, @Odd String p3, boolean b1) {
        String l1 = p1;
        
        if (b1) {
            l1 = p3;
        }
        @Odd String l3 = l1;
        
        while (b1) {
            l1 = p3;
        }
        @Odd String l4 = l1;
    }
    
    // return statement
    void t3(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
            return;
        }
        @Odd String l3 = l1;
    }
    
    // simple throw statement
    // TODO: activate, once throw is implemented
    /*void t4(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
            throw new RuntimeException();
        }
        @Odd String l3 = l1;
    }*/
    
    class C {
        String f1, f2, f3;
        @Odd String g1, g2, g3;
    }
    
    // fields
    void t3(@Odd String p1, String p2, boolean b1, C c1, C c2) {
        c1.f1 = p1;
        @Odd String l1 = c1.f1;
        c2.f2 = p2; // assignment to f2 does not change knowledge about f1
        c1.f2 = p2;
        @Odd String l2 = c1.f1;
        c2.f1 = p2;
        //:: error: (assignment.type.incompatible)
        @Odd String l3 = c1.f1;
    }
}
