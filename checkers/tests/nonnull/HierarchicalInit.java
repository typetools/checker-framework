
import checkers.nullness.quals.*;
import static checkers.nonnull.util.NonNullUtils.*;

class HierarchicalInit {
    
    String a;
    
    public HierarchicalInit() {
        a = "";
    }
    
    public static class B extends HierarchicalInit {
        String b;
        
        public B() {
            super();
            b = "";
        }
    }
    
}
