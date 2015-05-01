import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.initialization.qual.*;

public class Defaults {

    // local variable defaults
    void test(@UnknownInitialization Defaults para, @Initialized Defaults comm) {
        // @Nullable @UnknownInitialization by default
        String s = "abc";
        
        s = null;
        
        Defaults d;
        d = null; // null okay (default == @Nullable)
        
        d = comm; // committed okay (default == @Initialized)
        d.hashCode();
    }
}
