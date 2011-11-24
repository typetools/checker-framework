import checkers.nullness.quals.*;

class PureTest {
    @Pure @Nullable Object puremethod(@Nullable Object a) {
        return a;
    }

    public void test() {
        //:: error: (dereference.of.nullable)
        puremethod(null).toString();

        if (puremethod(null) == null) {
            //:: error: (dereference.of.nullable)
            puremethod(null).toString();
        }

        if (puremethod("m") != null) {
            puremethod("m").toString();
        }

        if (puremethod("m") != null) {
            //:: error: (dereference.of.nullable)
            puremethod(null).toString();
        }

        if (puremethod("m") != null) {
            //:: error: (dereference.of.nullable)
            puremethod("n").toString();
        }

        Object x = new Object();

        if (puremethod(x) == null) {
            return;
        }

        puremethod(x).toString();
        puremethod(x).toString();
        puremethod(x).toString();

        x = new Object();
        
        //:: error: (dereference.of.nullable)
        puremethod(x).toString();
        
        //:: error: (dereference.of.nullable)
        puremethod("n").toString();

    }

    public @Pure @Nullable Object getSuperclass() {
        return null;
    }

    static void shortCircuitAnd(PureTest pt) {
        if ((pt.getSuperclass() != null)
            && pt.getSuperclass().toString().equals("java.lang.Enum")) {
            // empty body
        }
    }

    static void shortCircuitOr(PureTest pt) {
        if ((pt.getSuperclass() == null)
            ||  pt.getSuperclass().toString().equals("java.lang.Enum")) {
            // empty body
        }
    }

    static void testInstanceofNegative(PureTest pt) {
        if (pt.getSuperclass() instanceof Object) {
            return;
        }
        //:: error: (dereference.of.nullable)
        pt.getSuperclass().toString();
    }

    static void testInstanceofPositive(PureTest pt) {
        if (!(pt.getSuperclass() instanceof Object)) {
            return;
        }
        pt.getSuperclass().toString();
    }

    static void testInstanceofPositive2(PureTest pt) {
        if (!(pt.getSuperclass() instanceof Object)) {
        } else {
            pt.getSuperclass().toString();
        }
    }

    static void testInstanceofNegative2(PureTest pt) {
        if (pt.getSuperclass() instanceof Object) {
        } else {
            return;
        }
        //:: error: (dereference.of.nullable)
        pt.getSuperclass().toString();
    }

    static void testInstanceofString(PureTest pt) {
        if (!(pt.getSuperclass() instanceof String)) {
            return;
        }
        pt.getSuperclass().toString();
    }

    static void testContinue(PureTest pt) {
        for (;;) {
            if (pt.getSuperclass() == null) {
                System.out.println("m");
                continue;
            }
            pt.getSuperclass().toString();
        }
    }

    void setSuperclass(@Nullable Object no) {
        // set the field returned by getSuperclass.
    }
    
    static void testInstanceofPositive3(PureTest pt) {
        if (!(pt.getSuperclass() instanceof Object)) {
            return;
        } else {
            pt.setSuperclass(null);
        }
        //:: error: (dereference.of.nullable)
        pt.getSuperclass().toString();
    }

}
