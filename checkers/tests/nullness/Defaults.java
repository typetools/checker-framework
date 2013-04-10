import checkers.nullness.quals.*;
import checkers.initialization.quals.*;

public class Defaults {

    // local variable defaults
    void test(@UnkownInitialization Defaults para, @Initialized Defaults comm) {
        // @Nullable @UnkownInitialization by default
        String s = "abc";
        
        s = null;
        
        Defaults d;
        d = null; // null okay (default == @Nullable)
        
        d = comm; // committed okay (default == @Initialized)
        d.hashCode();
    }
}
