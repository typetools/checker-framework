import checkers.nonnull.quals.*;
import static checkers.nonnull.util.NonNullUtils.*;

class SuperConstructorInit {
    
    String a;
    
    public SuperConstructorInit() {
        a = "";
    }
    
    public static class B extends SuperConstructorInit {
        String b;
        //:: error: (commitment.fields.uninitialized)
        public B() {
            super();
            a.toString();
        }
    }
    
}
