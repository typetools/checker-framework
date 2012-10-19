
import checkers.nonnull.quals.*;
import checkers.initialization.quals.*;

class MethodInvocation {

    String s;
    
    public MethodInvocation() {
        //:: error: (method.invocation.invalid)
        a();
        b();
        c();
        s = "abc";
    }
    
    public MethodInvocation(boolean p) {
        //:: error: (method.invocation.invalid)
        a(); // still not okay to be committed
        s = "abc";
    }
    
    public void a() {
    }

    public void b(@Free @Raw MethodInvocation this) {
        //:: error: (dereference.of.nullable)
        s.hashCode();
    }
    
    public void c(@Unclassified @Raw MethodInvocation this) {
        //:: error: (dereference.of.nullable)
        s.hashCode();
    }
}
