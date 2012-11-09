import java.lang.annotation.Annotation;
import java.util.ArrayList;

import checkers.nullness.quals.*;
import checkers.oigj.quals.O;
import static checkers.nonnull.util.NonNullUtils.*;;

class Unboxing {
    
    @Nullable Integer f;
    
    public void t1() {
        //:: error: (unboxing.of.nullable)
        @NonNull int l = f + 1;
        // no error, since f has been unboxed
        f.toString();
    }
    
}
