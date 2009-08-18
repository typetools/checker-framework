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
        if (s != null) { }
        else { }
        s.toString();
    }
    
    void testInvalidCase1() {
        String s = "m";
        if (s != null) { s = null; }
        else { }
        s.toString();   // error
    }

    void testInvalidCase2() {
        String s = "m";
        if (s != null) { }
        else { s = null; }
        s.toString();   // error
    }
}
