import checkers.nonnull.quals.*;
import checkers.commitment.quals.*;

public class Defaults {

    // local variable defaults
    void test(@Unclassified Defaults para, @Committed Defaults comm) {
        // @Nullable @Unclassified by default
        String s = "abc";
        
        s = null;
        
        Defaults d;
        d = null; // null okay (default == @Nullable)
        
        d = comm; // committed okay (default == @Committed)
        d.hashCode();
    }
}
