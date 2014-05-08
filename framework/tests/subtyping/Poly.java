import java.util.*;

import tests.util.*;

class Poly {

    void test() {

        @Encrypted String s = encrypt("as0d78f9(*#4j");
        String t = "foo";

        @Encrypted String x1 = id(s);   // valid
        //:: error: (assignment.type.incompatible)
        @Encrypted String x2 = id(t);   // error
        String x3 = id(s);              // valid
        String x4 = id(t);              // valid

        @Encrypted String y01 = combine(s, s);   // valid
        //:: error: (assignment.type.incompatible)
        @Encrypted String y02 = combine(s, t);   // error
        //:: error: (assignment.type.incompatible)
        @Encrypted String y03 = combine(t, t);   // error

        String y11 = combine(s, s);     // valid
        String y12 = combine(s, t);     // valid
        String y13 = combine(t, t);     // valid
    }

    @PolyEncrypted String id(@PolyEncrypted String s) {
        return s;
    }

    @PolyEncrypted String combine(@PolyEncrypted String s, @PolyEncrypted String t) {
        //:: error: (argument.type.incompatible)
        sendOverNet(s); // error
        return s;
    }

    void sendOverNet(@Encrypted String msg) {
    }

    List<@PolyEncrypted String> duplicate(@PolyEncrypted String s) {
        return null;
    }

    @PolyEncrypted String[] duplicateAsArray(@PolyEncrypted String s) {
        return null;
    }

    void test2() {
        @Encrypted String s = encrypt("p9aS*7dfa0w9e84r");
        List<@Encrypted String> lst = duplicate(s);
        @Encrypted String[] arr = duplicateAsArray(s);
    }

    @PolyEncrypted String substitute(Map<String, ? extends @PolyEncrypted String> map) {
        return encrypt(null);
    }

    @PolyEncrypted String substituteSuper(Map<String, ? super @PolyEncrypted String> map) {
        return encrypt(null);
    }

    void test3() {
        //:: error: (assignment.type.incompatible)
        @Encrypted String s = substitute(new HashMap<String, String>());
        @Encrypted String t = substitute(new HashMap<String, @Encrypted String>());

        //:: error: (assignment.type.incompatible)
        @Encrypted String q = substituteSuper(new HashMap<String, String>());
        @Encrypted String r = substituteSuper(new HashMap<String, @Encrypted String>());
    }

    // Test assignment to poly
    @PolyEncrypted String test4(@PolyEncrypted String s) {
        if (s == null)
            return encrypt(null);  // valid
        else
            //:: error: (return.type.incompatible)
            return "m";  // invalid
    }

    @SuppressWarnings("encrypted")
    static @Encrypted String encrypt(String s) {
        return (@Encrypted String) s;
    }
}
