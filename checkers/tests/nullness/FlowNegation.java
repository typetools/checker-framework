public class FlowNegation {

    void testSimpleValid() {
        String s = "m";
        s.toString();
    }

    void testCase1() {
        String s = "m";
        if (s != null) { }
        else { }
        s.toString();
    }

    void testCase2() {
        String s = "m";
        if (s == null) { }
        else { }
        s.toString();
    }

    void testInvalidCase1() {
        String s = "m";
        if (s != null) { s = null; }
        else { }
        //:: error: (dereference.of.nullable)
        s.toString();   // error
    }

    void testInvalidCase2() {
        String s = "m";
        if (s != null) { }
        else { s = null; }
        //:: error: (dereference.of.nullable)
        s.toString();   // error
    }

    void testSimpleValidTernary() {
        String s = "m";
        s.toString();
    }

    void testTernaryCase1() {
        String s = "m";
        Object m = (s != null) ? "m" : "n";
        s.toString();
    }

    void testTernaryCase2() {
        String s = "m";
        Object m = (s == null) ? "m" : "n";
        s.toString();
    }

    void testTernaryInvalidCase1() {
        String s = "m";
        Object m = (s != null) ? (s = null) : "n";
        //:: error: (dereference.of.nullable)
        s.toString();   // error
    }

    void testTernaryInvalidCase2() {
        String s = "m";
        Object m = (s != null) ? "m" : (s = null);
        //:: error: (dereference.of.nullable)
        s.toString();   // error
    }

    void testAssignInCond() {
        String s = "m";
        if ((s = null) != "m") {
          //:: error: (dereference.of.nullable)
            s.toString();   // error
        } else {
        }
        //:: error: (dereference.of.nullable)
        s.toString();   // error
    }
}
