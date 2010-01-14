import checkers.nullness.quals.*;

class PureTest {
    @Pure @Nullable Object puremethod(@Nullable Object a) {
        return a;
    }

    public void test() {
        //:: (dereference.of.nullable)
        puremethod(null).toString();

        if (puremethod(null) == null) {
            //:: (dereference.of.nullable)
            puremethod(null).toString();
        }

        if (puremethod("m") != null) {
            puremethod("m").toString();
        }

        if (puremethod("m") != null) {
            //:: (dereference.of.nullable)
            puremethod(null).toString();
        }

        if (puremethod("m") != null) {
            //:: (dereference.of.nullable)
            puremethod("n").toString();
        }

        Object x = new Object();

        if (puremethod(x) == null) {
            return;
        }

        puremethod(x).toString();
        puremethod(x).toString();

        x = new Object();
        //:: (dereference.of.nullable)
        puremethod("n").toString();

    }
}
