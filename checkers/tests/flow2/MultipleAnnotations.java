import checkers.util.test.*;
import tests.util.*;

import java.util.*;
import checkers.quals.*;

// some tests with several annotations
class MultipleAnnotations {
    
    void t1(@Odd @Value String p1) {
        @Value String l1 = p1;
        @Odd String l2 = p1;
        @Odd @Value String l3 = p1;
        @Value @Odd String l4 = p1;
    }
    
    void t2(@Odd @Value String p1) {
        @Value String l1 = p1;
        @Odd String l2 = l1;
        @Odd @Value String l3 = l1;
        @Value @Odd String l4 = l1;
    }
    
}
