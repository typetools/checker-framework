import checkers.nullness.quals.*;

public class FlowLoop {
    void simpleWhileLoop() {
        String s = "m";

        while (s != null) {
            s.toString();
            s = null;
        }
        //:: (dereference.of.nullable)
        s.toString();   // error
    }

    void whileConditionError() {
        String s = "m";

        //:: (dereference.of.nullable)
        while (s.toString() == "m") {  // error
            s.toString();
            s = null;
        }
        s.toString();
    }

    void simpleForLoop() {
        for (String s = "m"; s != null; s = null) {
            s.toString();
        }
    }

    void forLoopConditionError() {
        for (String s = "m";
             //:: (dereference.of.nullable)
             s.toString() != "m";   // error
             s = null) {
            s.toString();
        }
    }

    class Link {
        Object val;
        @Nullable Link next;
    }

    // Both dereferences of l should succeed
    void test(@Nullable Link in) {
        for (@Nullable Link l=in; l!=null; l=l.next) {
            Object o;
            o = l.val;
        }
    }

    void multipleRuns() {
        String s = "m";
        while (true) {
            //:: (dereference.of.nullable)
            s.toString();   // error
            s = null;
        }
    }

    void multipleRunsDo() {
        String s = "m";
        do {
            //:: (dereference.of.nullable)
            s.toString();   // error
            s = null;
        } while (true);
    }

    void alwaysRunForLoop() {
        String s = "m";
        for (s = null; s != null; s = "m") {
            s.toString();   // ok
        }
        //:: (dereference.of.nullable)
        s.toString();   // error
    }

    public void badIterator () {
        Class<?> opt_doc1 = null;
        //:: (dereference.of.nullable)
        opt_doc1.getInterfaces();
        Class<?> opt_doc2 = null;
        //:: (dereference.of.nullable)
        for (Class<?> fd : opt_doc2.getInterfaces()) {
            // empty loop body
        }
    }

    void testContinue(@Nullable Object o) {
        for (;;) {
            //:: (dereference.of.nullable)
            o.toString();
            if (true) continue;
        }
    }

    void testBreak(@Nullable Object o) {
        while (true) {
            //:: (dereference.of.nullable)
            o.toString();
            if (true) break;
        }
    }

    void testSimpleNull() {
        String r1 = null;
        while (r1 != null);
        //:: (dereference.of.nullable)
        r1.toString();  // error
    }

    void testMulticheckNull() {
        String r1 = null;
        while (r1 != null && r1.equals("m"));
        //:: (dereference.of.nullable)
        r1.toString();  // error
    }

    void testAssignInLoopSimple() {
        String r1 = "";
        while (r1 != null) {
            r1 = null;
        }
        //:: (dereference.of.nullable)
        r1.toString();  // error
    }

    void testAssignInLoopMulti() {
        String r1 = "";
        while (r1 != null && r1.isEmpty()) {
            r1 = null;
        }
        //:: (dereference.of.nullable)
        r1.toString();  // error
    }

    void testBreakWithCheck() {
        String s = null;
        while (true) {
            if (s == null) break;
            s.toString();
        }
    }

    void test1() {
        while (true) {
            String s = null;
            if (s == null)
                return;
            s.toString();
        }
    }

}
