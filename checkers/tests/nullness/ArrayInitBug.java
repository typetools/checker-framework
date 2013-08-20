import java.lang.annotation.Annotation;
import java.util.ArrayList;

import checkers.nullness.quals.*;
import checkers.oigj.quals.O;

class ArrayInitBug {
    
    @Nullable Object @Nullable [] aa;
    
    public ArrayInitBug() {
        aa = null;
    }
    
}
