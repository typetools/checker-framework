package hardcoded;

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

@HasQualifierParameter(Tainted.class)
public class Refine {
    void method(@Tainted Refine tainted, @Untainted Refine untainted) {
        // :: error: (assignment.type.incompatible)
        @Tainted Refine local = untainted;
        // :: error: (assignment.type.incompatible)
        @Untainted Refine untaintedLocal = local;
        @Untainted Refine untaintedLocal2 = untaintedLocal;
    }
}
