
import checkers.nonnull.quals.*;
import checkers.initialization.quals.*;

public class Suppression {
    
    @NonNull Suppression t;
    
    @SuppressWarnings("commitment.fields.uninitialized")
    public Suppression(Suppression arg) {
    
    }
    
    @SuppressWarnings({"commitment","nonnull"})
    void foo(@Unclassified Suppression arg) {
        t = arg;    // "commitment" error
        t = null;    // "nonnull" error
    }
    
    void test() {
        @SuppressWarnings("nonnullfbc:assignment.type.incompatible")
        @NonNull String s = null;
    }

}
