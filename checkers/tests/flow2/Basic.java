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
        @Odd String l2 = l1;
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
        @Odd String l2 = l1;
        
        if (b1) {
            l1 = p3;
        }
        @Odd String l3 = l1;
        
        while (b1) {
            l1 = p3;
        }
        @Odd String l4 = l1;
    }
}
