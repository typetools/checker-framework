import checkers.nullness.quals.*;

class FlowConditions {
    void m(@Nullable Object x, @Nullable Object y) {
        if (x == null || y == null) {
            //:: error: (dereference.of.nullable)
            x.toString();
            //:: error: (dereference.of.nullable)            
            y.toString();            
        } else {
            x.toString();
            y.toString();
        }
    }
}