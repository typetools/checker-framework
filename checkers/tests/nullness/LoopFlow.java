import checkers.nullness.quals.*;

public class LoopFlow {
    void simpleWhileLoop() {
        String s = "m";

        while (s != null) {
            s.toString();
            s = null;
        }
        s.toString();   // error
    }

    void whileConditionError() {
        String s = "m";

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
            s.toString();   // error
            s = null;
        }
    }

    void multipleRunsDo() {
        String s = "m";
        do {
            s.toString();   // error
            s = null;
        } while (true);
    }

    void alwaysRunForLoop() {
        String s = "m";
        for (s = null; s != null; s = "m") {
            s.toString();   // ok
        }
        s.toString();   // error
    }
}
