import checkers.nullness.quals.*;

class PureTest {
    @Pure @Nullable Object a(@Nullable Object a) {
        return a;
    }

    public void test() {
        //:: (dereference.of.nullable)
        a(null).toString();
        
        if (a(null) == null) {
            //:: (dereference.of.nullable)
            a(null).toString();
        }
        
        if (a("m") != null) {
            a("m").toString();
        }

        if (a("m") != null) {
            //:: (dereference.of.nullable)
            a(null).toString();
        }
    }
}