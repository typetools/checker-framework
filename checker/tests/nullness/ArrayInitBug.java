import java.lang.annotation.Annotation;
import java.util.ArrayList;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.oigj.qual.O;

class ArrayInitBug {
    
    @Nullable Object @Nullable [] aa;
    
    public ArrayInitBug() {
        aa = null;
    }
    
}
