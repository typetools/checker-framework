
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class Flow {
    
    @NonNull String f;
    @NotOnlyInitialized @NonNull String g;
    
    public Flow(String arg) {
        //:: error: (dereference.of.nullable)
        f.toLowerCase();
        //:: error: (dereference.of.nullable)
        g.toLowerCase();
        f = arg;
        g = arg;
        foo();
        f.toLowerCase();
        //:: error: (method.invocation.invalid)
        g.toLowerCase();
        f = arg;
    }
    
    void test() {
        @Nullable String s = null;
        s = "a";
        s.toLowerCase();
    }
    
    void test2(@Nullable String s) {
        if (s != null) {
            s.toLowerCase();
        }
    }
    
    void foo(@UnknownInitialization Flow this) {}
    
    // TODO Pure, etc.
}
