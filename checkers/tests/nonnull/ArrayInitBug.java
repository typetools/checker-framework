import java.lang.annotation.Annotation;
import java.util.ArrayList;

import checkers.nullness.quals.*;
import checkers.oigj.quals.O;
import static checkers.nonnull.util.NonNullUtils.*;;

class ArrayInitBug {
    
    @Nullable Object @Nullable [] aa;
    
    public ArrayInitBug() {
        aa = null;
    }
    
}
